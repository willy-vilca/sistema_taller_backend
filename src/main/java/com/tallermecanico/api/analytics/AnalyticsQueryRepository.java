package com.tallermecanico.api.analytics;

import com.tallermecanico.api.service.ServiceRecord;
import com.tallermecanico.api.common.SearchNormalizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class AnalyticsQueryRepository {
    private final EntityManager entityManager;

    public AnalyticsQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<ServiceRecord> searchServiceRecords(AnalyticsServiceFilters filters, int page, int size) {
        String whereClause = buildWhereClause(filters);
        String dataQuery = "select serviceRecord " + sourceClause(true) + whereClause + " order by " + orderBy(filters.sort());
        TypedQuery<ServiceRecord> query = entityManager.createQuery(dataQuery, ServiceRecord.class);
        applyFilters(query, filters);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(serviceRecord) " + sourceClause(false) + whereClause,
                Long.class
        );
        applyFilters(countQuery, filters);
        long total = countQuery.getSingleResult();
        return new PageImpl<>(query.getResultList(), PageRequest.of(page, size), total);
    }

    public AnalyticsTotals summarize(AnalyticsServiceFilters filters) {
        String whereClause = buildWhereClause(filters);
        TypedQuery<Object[]> totalsQuery = entityManager.createQuery(
                "select count(serviceRecord), sum(serviceRecord.totalCost), avg(serviceRecord.totalCost) " + sourceClause(false) + whereClause,
                Object[].class
        );
        applyFilters(totalsQuery, filters);
        Object[] totals = totalsQuery.getSingleResult();

        TypedQuery<Long> scheduledCountQuery = entityManager.createQuery(
                "select count(serviceRecord) " + sourceClause(false)
                        + whereClause
                        + " and exists (select scheduledService.id from ScheduledService scheduledService where scheduledService.completedServiceRecord = serviceRecord)",
                Long.class
        );
        applyFilters(scheduledCountQuery, filters);

        return new AnalyticsTotals(
                number(totals[0]).longValue(),
                decimal(totals[1]),
                decimal(totals[2]),
                scheduledCountQuery.getSingleResult()
        );
    }

    public List<AnalyticsTrendResponse> findDailyTrend(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select serviceRecord.serviceDate, count(serviceRecord), sum(serviceRecord.totalCost)
                        from ServiceRecord serviceRecord
                        where serviceRecord.serviceDate between :fromDate and :toDate
                        group by serviceRecord.serviceDate
                        order by serviceRecord.serviceDate asc
                        """, Object[].class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList()
                .stream()
                .map(row -> new AnalyticsTrendResponse(
                        ((LocalDate) row[0]).toString(),
                        number(row[1]).longValue(),
                        decimal(row[2])
                ))
                .toList();
    }

    public List<AnalyticsRankingResponse> findTopClients(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select client.id, client.fullName, count(serviceRecord), sum(serviceRecord.totalCost)
                        from ServiceRecord serviceRecord
                        join serviceRecord.vehicle vehicle
                        join vehicle.client client
                        where serviceRecord.serviceDate between :fromDate and :toDate
                        group by client.id, client.fullName
                        order by sum(serviceRecord.totalCost) desc nulls last, count(serviceRecord) desc
                        """, Object[].class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(row -> ranking((UUID) row[0], (String) row[1], null, row[2], row[3]))
                .toList();
    }

    public List<AnalyticsRankingResponse> findTopVehicles(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select vehicle.id, vehicle.licensePlate, vehicle.model, client.fullName, count(serviceRecord), sum(serviceRecord.totalCost)
                        from ServiceRecord serviceRecord
                        join serviceRecord.vehicle vehicle
                        join vehicle.client client
                        where serviceRecord.serviceDate between :fromDate and :toDate
                        group by vehicle.id, vehicle.licensePlate, vehicle.model, client.fullName
                        order by sum(serviceRecord.totalCost) desc nulls last, count(serviceRecord) desc
                        """, Object[].class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(row -> ranking(
                        (UUID) row[0],
                        (String) row[1],
                        row[2] + " · " + row[3],
                        row[4],
                        row[5]
                ))
                .toList();
    }

    public List<AnalyticsRankingResponse> findTopEmployees(LocalDate fromDate, LocalDate toDate) {
        return entityManager.createQuery("""
                        select responsibleUser.id, responsibleUser.fullName, count(serviceRecord), sum(serviceRecord.totalCost)
                        from ServiceRecord serviceRecord
                        join serviceRecord.responsibleUser responsibleUser
                        where serviceRecord.serviceDate between :fromDate and :toDate
                        group by responsibleUser.id, responsibleUser.fullName
                        order by sum(serviceRecord.totalCost) desc nulls last, count(serviceRecord) desc
                        """, Object[].class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(row -> ranking((UUID) row[0], (String) row[1], null, row[2], row[3]))
                .toList();
    }

    private String buildWhereClause(AnalyticsServiceFilters filters) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        if (filters.search() != null && !filters.search().isBlank()) {
            where.append(" and (lower(client.fullName) like :searchPattern")
                    .append(" or replace(replace(lower(vehicle.licensePlate), '-', ''), ' ', '') like :plateSearchPattern")
                    .append(" or lower(vehicle.model) like :searchPattern")
                    .append(" or lower(serviceRecord.description) like :searchPattern")
                    .append(" or lower(responsibleUser.fullName) like :searchPattern)");
        }
        if (filters.clientId() != null) where.append(" and client.id = :clientId");
        if (filters.vehicleId() != null) where.append(" and vehicle.id = :vehicleId");
        if (filters.responsibleUserId() != null) where.append(" and responsibleUser.id = :responsibleUserId");
        if (filters.fromDate() != null) where.append(" and serviceRecord.serviceDate >= :fromDate");
        if (filters.toDate() != null) where.append(" and serviceRecord.serviceDate <= :toDate");
        if (filters.minCost() != null) where.append(" and serviceRecord.totalCost >= :minCost");
        if (filters.maxCost() != null) where.append(" and serviceRecord.totalCost <= :maxCost");
        if (filters.hasNextService() != null) {
            where.append(filters.hasNextService() ? " and serviceRecord.nextServiceDate is not null" : " and serviceRecord.nextServiceDate is null");
        }
        if (filters.origin() == AnalyticsServiceOrigin.SCHEDULED) {
            where.append(" and exists (select scheduledService.id from ScheduledService scheduledService where scheduledService.completedServiceRecord = serviceRecord)");
        }
        if (filters.origin() == AnalyticsServiceOrigin.NEW) {
            where.append(" and not exists (select scheduledService.id from ScheduledService scheduledService where scheduledService.completedServiceRecord = serviceRecord)");
        }
        return where.toString();
    }

    private String sourceClause(boolean fetch) {
        if (fetch) {
            return "from ServiceRecord serviceRecord "
                    + "join fetch serviceRecord.vehicle vehicle "
                    + "join fetch vehicle.client client "
                    + "join fetch serviceRecord.responsibleUser responsibleUser ";
        }
        return "from ServiceRecord serviceRecord "
                + "join serviceRecord.vehicle vehicle "
                + "join vehicle.client client "
                + "join serviceRecord.responsibleUser responsibleUser ";
    }

    private void applyFilters(Query query, AnalyticsServiceFilters filters) {
        if (filters.search() != null && !filters.search().isBlank()) {
            query.setParameter("searchPattern", "%" + SearchNormalizer.text(filters.search()) + "%");
            query.setParameter("plateSearchPattern", "%" + SearchNormalizer.plate(filters.search()) + "%");
        }
        if (filters.clientId() != null) query.setParameter("clientId", filters.clientId());
        if (filters.vehicleId() != null) query.setParameter("vehicleId", filters.vehicleId());
        if (filters.responsibleUserId() != null) query.setParameter("responsibleUserId", filters.responsibleUserId());
        if (filters.fromDate() != null) query.setParameter("fromDate", filters.fromDate());
        if (filters.toDate() != null) query.setParameter("toDate", filters.toDate());
        if (filters.minCost() != null) query.setParameter("minCost", filters.minCost());
        if (filters.maxCost() != null) query.setParameter("maxCost", filters.maxCost());
    }

    private String orderBy(AnalyticsServiceSort sort) {
        AnalyticsServiceSort resolvedSort = sort == null ? AnalyticsServiceSort.SERVICE_DATE_DESC : sort;
        return switch (resolvedSort) {
            case SERVICE_DATE_ASC -> "serviceRecord.serviceDate asc, serviceRecord.createdAt asc";
            case COST_DESC -> "serviceRecord.totalCost desc nulls last, serviceRecord.serviceDate desc";
            case COST_ASC -> "serviceRecord.totalCost asc nulls last, serviceRecord.serviceDate desc";
            case SERVICE_DATE_DESC -> "serviceRecord.serviceDate desc, serviceRecord.createdAt desc";
        };
    }

    private AnalyticsRankingResponse ranking(UUID id, String label, String secondaryLabel, Object serviceCount, Object revenue) {
        return new AnalyticsRankingResponse(id, label, secondaryLabel, number(serviceCount).longValue(), decimal(revenue));
    }

    private Number number(Object value) {
        return value == null ? 0 : (Number) value;
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }
}
