package com.tallermecanico.api.analytics;

import com.tallermecanico.api.client.ClientRepository;
import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.common.PageResponse;
import com.tallermecanico.api.scheduledservice.ScheduledServiceMapper;
import com.tallermecanico.api.scheduledservice.ScheduledServiceRepository;
import com.tallermecanico.api.service.ServiceRecordMapper;
import com.tallermecanico.api.user.SystemUserRepository;
import com.tallermecanico.api.vehicle.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    private final AnalyticsQueryRepository analyticsQueryRepository;
    private final ScheduledServiceRepository scheduledServiceRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final SystemUserRepository userRepository;

    public AnalyticsService(
            AnalyticsQueryRepository analyticsQueryRepository,
            ScheduledServiceRepository scheduledServiceRepository,
            ClientRepository clientRepository,
            VehicleRepository vehicleRepository,
            SystemUserRepository userRepository
    ) {
        this.analyticsQueryRepository = analyticsQueryRepository;
        this.scheduledServiceRepository = scheduledServiceRepository;
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    public AnalyticsDashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate) {
        DateRange range = resolveDashboardRange(fromDate, toDate);
        AnalyticsTotals totals = analyticsQueryRepository.summarize(new AnalyticsServiceFilters(
                null, null, null, null, range.fromDate(), range.toDate(), null, null, null, null, null
        ));
        long newServiceCount = totals.serviceCount() - totals.scheduledCompletionCount();

        return new AnalyticsDashboardResponse(
                range.fromDate(),
                range.toDate(),
                totals.serviceCount(),
                totals.revenue(),
                totals.averageTicket(),
                newServiceCount,
                totals.scheduledCompletionCount(),
                scheduledServiceRepository.countByCompletedServiceRecordIsNull(),
                scheduledServiceRepository.countByCompletedServiceRecordIsNullAndScheduledDateBefore(LocalDate.now()),
                buildTrend(analyticsQueryRepository.findDailyTrend(range.fromDate(), range.toDate()), range),
                analyticsQueryRepository.findTopClients(range.fromDate(), range.toDate()),
                analyticsQueryRepository.findTopVehicles(range.fromDate(), range.toDate()),
                analyticsQueryRepository.findTopEmployees(range.fromDate(), range.toDate()),
                scheduledServiceRepository.findTop5ByCompletedServiceRecordIsNullOrderByScheduledDateAsc()
                        .stream()
                        .map(ScheduledServiceMapper::toResponse)
                        .toList()
        );
    }

    public PageResponse<AnalyticsServiceRecordResponse> searchServices(AnalyticsServiceFilters filters, int page, int size) {
        validateFilters(filters);
        Page<com.tallermecanico.api.service.ServiceRecord> result = analyticsQueryRepository.searchServiceRecords(filters, page, size);
        Map<UUID, UUID> scheduledServiceIdsByRecordId = completedScheduledServiceIds(result.getContent().stream().map(com.tallermecanico.api.service.ServiceRecord::getId).toList());

        return PageResponse.from(result, serviceRecord -> {
            UUID scheduledServiceId = scheduledServiceIdsByRecordId.get(serviceRecord.getId());
            return new AnalyticsServiceRecordResponse(
                    ServiceRecordMapper.toResponse(serviceRecord),
                    scheduledServiceId == null ? AnalyticsServiceOrigin.NEW : AnalyticsServiceOrigin.SCHEDULED,
                    scheduledServiceId
            );
        });
    }

    public AnalyticsSummaryResponse summarizeServices(AnalyticsServiceFilters filters) {
        validateFilters(filters);
        AnalyticsTotals totals = analyticsQueryRepository.summarize(filters);
        return new AnalyticsSummaryResponse(
                totals.serviceCount(),
                totals.revenue(),
                totals.averageTicket(),
                totals.serviceCount() - totals.scheduledCompletionCount(),
                totals.scheduledCompletionCount()
        );
    }

    public AnalyticsFilterOptionsResponse getFilterOptions() {
        return new AnalyticsFilterOptionsResponse(
                clientRepository.findAllByOrderByFullNameAsc().stream()
                        .map(client -> new AnalyticsFilterOptionsResponse.ClientOption(client.getId(), client.getFullName(), client.getDni()))
                        .toList(),
                vehicleRepository.findAllByOrderByLicensePlateAsc().stream()
                        .map(vehicle -> new AnalyticsFilterOptionsResponse.VehicleOption(
                                vehicle.getId(),
                                vehicle.getClient().getId(),
                                vehicle.getClient().getFullName(),
                                vehicle.getLicensePlate(),
                                vehicle.getModel()
                        ))
                        .toList(),
                userRepository.findAllByOrderByFullNameAsc().stream()
                        .map(user -> new AnalyticsFilterOptionsResponse.EmployeeOption(
                                user.getId(), user.getFullName(), user.getUsername(), user.isActive()
                        ))
                        .toList()
        );
    }

    private Map<UUID, UUID> completedScheduledServiceIds(List<UUID> serviceRecordIds) {
        if (serviceRecordIds.isEmpty()) return Map.of();
        Map<UUID, UUID> links = new HashMap<>();
        for (Object[] link : scheduledServiceRepository.findCompletedServiceLinks(serviceRecordIds)) {
            links.put((UUID) link[0], (UUID) link[1]);
        }
        return links;
    }

    private List<AnalyticsTrendResponse> buildTrend(List<AnalyticsTrendResponse> dailyTrend, DateRange range) {
        long numberOfDays = ChronoUnit.DAYS.between(range.fromDate(), range.toDate()) + 1;
        if (numberOfDays <= 62) {
            Map<LocalDate, AnalyticsTrendResponse> byDate = new HashMap<>();
            dailyTrend.forEach(point -> byDate.put(LocalDate.parse(point.period()), point));
            return range.fromDate().datesUntil(range.toDate().plusDays(1))
                    .map(date -> byDate.getOrDefault(date, new AnalyticsTrendResponse(date.toString(), 0, BigDecimal.ZERO)))
                    .toList();
        }

        Map<YearMonth, TrendAccumulator> byMonth = new HashMap<>();
        dailyTrend.forEach(point -> {
            YearMonth month = YearMonth.from(LocalDate.parse(point.period()));
            byMonth.merge(month, new TrendAccumulator(point.serviceCount(), point.revenue()), TrendAccumulator::add);
        });
        return range.fromDate().withDayOfMonth(1).datesUntil(range.toDate().withDayOfMonth(1).plusMonths(1), java.time.Period.ofMonths(1))
                .map(YearMonth::from)
                .map(month -> {
                    TrendAccumulator value = byMonth.getOrDefault(month, new TrendAccumulator(0, BigDecimal.ZERO));
                    return new AnalyticsTrendResponse(month.toString(), value.serviceCount(), value.revenue());
                })
                .toList();
    }

    private DateRange resolveDashboardRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedToDate = toDate == null ? LocalDate.now() : toDate;
        LocalDate resolvedFromDate = fromDate == null ? resolvedToDate.minusDays(29) : fromDate;
        if (resolvedFromDate.isAfter(resolvedToDate)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }
        return new DateRange(resolvedFromDate, resolvedToDate);
    }

    private void validateFilters(AnalyticsServiceFilters filters) {
        if (filters.fromDate() != null && filters.toDate() != null && filters.fromDate().isAfter(filters.toDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (filters.minCost() != null && filters.maxCost() != null && filters.minCost().compareTo(filters.maxCost()) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El costo mínimo no puede ser mayor que el costo máximo.");
        }
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private record TrendAccumulator(long serviceCount, BigDecimal revenue) {
        private TrendAccumulator add(TrendAccumulator other) {
            return new TrendAccumulator(serviceCount + other.serviceCount, revenue.add(other.revenue));
        }
    }
}
