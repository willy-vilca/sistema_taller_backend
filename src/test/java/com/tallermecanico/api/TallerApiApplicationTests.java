package com.tallermecanico.api;

import com.tallermecanico.api.client.Client;
import com.tallermecanico.api.client.ClientRepository;
import com.tallermecanico.api.common.BusinessException;
import com.tallermecanico.api.analytics.AnalyticsService;
import com.tallermecanico.api.analytics.AnalyticsServiceFilters;
import com.tallermecanico.api.analytics.AnalyticsServiceOrigin;
import com.tallermecanico.api.scheduledservice.ScheduledServiceRequest;
import com.tallermecanico.api.scheduledservice.ScheduledServiceResponse;
import com.tallermecanico.api.scheduledservice.ScheduledServiceService;
import com.tallermecanico.api.scheduledservice.ScheduledServiceStatus;
import com.tallermecanico.api.service.ServiceRecordRequest;
import com.tallermecanico.api.service.ServiceRecordResponse;
import com.tallermecanico.api.service.ServiceRecordService;
import com.tallermecanico.api.user.Role;
import com.tallermecanico.api.user.RoleName;
import com.tallermecanico.api.user.RoleRepository;
import com.tallermecanico.api.user.SystemUser;
import com.tallermecanico.api.user.SystemUserRepository;
import com.tallermecanico.api.vehicle.Vehicle;
import com.tallermecanico.api.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TallerApiApplicationTests {

	@Autowired
	private ScheduledServiceService scheduledServiceService;

	@Autowired
	private ServiceRecordService serviceRecordService;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private SystemUserRepository userRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private AnalyticsService analyticsService;

	@Test
	void contextLoads() {
	}

	@Test
	void completesScheduledServiceAndCreatesNextSchedule() {
		Role role = roleRepository.findByName(RoleName.EMPLEADO).orElseThrow();
		SystemUser employee = userRepository.save(new SystemUser("mecanico", "Mecánico de prueba", "hash", role));
		Client client = clientRepository.save(new Client("Cliente de prueba", "12345678", null, null));
		Vehicle vehicle = vehicleRepository.save(new Vehicle(client, "ABC-123", "Toyota Yaris"));
		LocalDate serviceDate = LocalDate.now();

		ScheduledServiceResponse scheduledService = scheduledServiceService.create(
				new ScheduledServiceRequest(vehicle.getId(), serviceDate.plusDays(2), "Cambio de aceite"),
				"mecanico"
		);

		ServiceRecordResponse completedService = serviceRecordService.create(
				new ServiceRecordRequest(
						vehicle.getId(),
						employee.getId(),
						"Cambio de aceite realizado",
						serviceDate,
						serviceDate.plusDays(90),
						"Mantenimiento preventivo",
						scheduledService.id(),
						new BigDecimal("85.00"),
						null
				),
				"mecanico"
		);

		ScheduledServiceResponse completedSchedule = scheduledServiceService.get(scheduledService.id());
		var pendingSchedules = scheduledServiceService.search(null, vehicle.getId(), null, null, ScheduledServiceStatus.PENDING, 0, 20);

		assertThat(completedSchedule.status()).isEqualTo(ScheduledServiceStatus.COMPLETED);
		assertThat(completedSchedule.completedServiceRecordId()).isEqualTo(completedService.id());
		assertThatThrownBy(() -> scheduledServiceService.deletePending(scheduledService.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("no se pueden editar ni eliminar");
		assertThat(pendingSchedules.content()).singleElement().satisfies(nextSchedule -> {
			assertThat(nextSchedule.sourceServiceRecordId()).isEqualTo(completedService.id());
			assertThat(nextSchedule.description()).isEqualTo("Mantenimiento preventivo");
			assertThat(nextSchedule.scheduledDate()).isEqualTo(serviceDate.plusDays(90));
		});

		var dashboard = analyticsService.getDashboard(serviceDate, serviceDate);
		var analyticsResults = analyticsService.searchServices(
				new AnalyticsServiceFilters(null, client.getId(), vehicle.getId(), employee.getId(), serviceDate, serviceDate, null, null, AnalyticsServiceOrigin.SCHEDULED, null, null),
				0,
				20
		);

		assertThat(dashboard.serviceCount()).isEqualTo(1);
		assertThat(dashboard.revenue()).isEqualByComparingTo("85.00");
		assertThat(dashboard.scheduledCompletionCount()).isEqualTo(1);
		assertThat(dashboard.topClients()).singleElement().extracting("label").isEqualTo("Cliente de prueba");
		assertThat(analyticsResults.content()).singleElement().satisfies(result -> {
			assertThat(result.origin()).isEqualTo(AnalyticsServiceOrigin.SCHEDULED);
			assertThat(result.service().id()).isEqualTo(completedService.id());
		});
	}

	@Test
	void updatesAndDeletesPendingScheduledService() {
		Role role = roleRepository.findByName(RoleName.EMPLEADO).orElseThrow();
		SystemUser employee = userRepository.save(new SystemUser("mecanico-edicion", "Mecánico de edición", "hash", role));
		Client client = clientRepository.save(new Client("Cliente de edición", "87654321", null, null));
		Vehicle originalVehicle = vehicleRepository.save(new Vehicle(client, "EDT-001", "Kia Rio"));
		Vehicle replacementVehicle = vehicleRepository.save(new Vehicle(client, "EDT-002", "Toyota Yaris"));

		ScheduledServiceResponse created = scheduledServiceService.create(
				new ScheduledServiceRequest(originalVehicle.getId(), LocalDate.now().plusDays(8), "Cambio de aceite"),
				"mecanico-edicion"
		);

		ScheduledServiceResponse updated = scheduledServiceService.update(
				created.id(),
				new ScheduledServiceRequest(replacementVehicle.getId(), LocalDate.now().plusDays(12), "Revisión integral")
		);

		assertThat(updated.status()).isEqualTo(ScheduledServiceStatus.PENDING);
		assertThat(updated.vehicleId()).isEqualTo(replacementVehicle.getId());
		assertThat(updated.scheduledDate()).isEqualTo(LocalDate.now().plusDays(12));
		assertThat(updated.description()).isEqualTo("Revisión integral");

		scheduledServiceService.deletePending(updated.id());

		assertThat(scheduledServiceService.search(null, replacementVehicle.getId(), null, null, null, 0, 20).content())
				.noneMatch(scheduledService -> scheduledService.id().equals(updated.id()));
	}

}
