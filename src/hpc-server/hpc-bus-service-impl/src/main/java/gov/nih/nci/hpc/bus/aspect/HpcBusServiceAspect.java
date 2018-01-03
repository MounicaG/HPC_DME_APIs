/**
 * HpcBusServiceAspect.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.aspect;

import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;

/**
 * HPC Bus Services Aspect - implement cross cutting concerns: 1. Basic business service profiler -
 * log execution time. 2. Exception logger - logging when exceptions are thrown by Bus Services API
 * impl. 3. Notify System Administrator if an error occurred with an integrated system (iRODS, LDAP,
 * CLEVERSAFE, GLOBUS, POSTGRESQL)
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Aspect
public class HpcBusServiceAspect {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Application service instances.

  @Autowired private HpcNotificationService notificationService = null;

  // The system administrator NCI user ID.
  @Value("${hpc.bus.aspect.systemAdministratorUserId}")
  private String systemAdministratorUserId = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcBusServiceAspect() {}

  //---------------------------------------------------------------------//
  // Pointcuts.
  //---------------------------------------------------------------------//

  /**
   * Join Point for all business services that are defined by an interface in the
   * gov.nih.nci.hpc.bus package, and implemented by a concrete class in gov.nih.nci.hpc.bus.impl
   */
  @Pointcut("within(gov.nih.nci.hpc.bus.impl.*) && execution(* gov.nih.nci.hpc.bus.*.*(..))")
  private void busServices() {
    // Intentionally left blank.
  }

  //---------------------------------------------------------------------//
  // Advices.
  //---------------------------------------------------------------------//

  /**
   * Advice that logs business service execution time.
   *
   * @param joinPoint The join point.
   * @return The advised object return.
   * @throws Throwable The advised object exception.
   */
  @Around("busServices()")
  public Object profileService(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    String businessService = joinPoint.getSignature().toShortString();
    logger.info("{} business service invoked.", businessService);

    try {
      return joinPoint.proceed();

    } finally {
      long executionTime = System.currentTimeMillis() - start;
      logger.debug(
          "{} business service completed in {} milliseconds.", businessService, executionTime);
    }
  }

  /**
   * Advice that logs business service exception.
   *
   * @param joinPoint The join point.
   * @param exception The exception to log.
   */
  @AfterThrowing(pointcut = "busServices()", throwing = "exception")
  public void logException(JoinPoint joinPoint, HpcException exception) {
    String businessService = joinPoint.getSignature().toShortString();
    logger.error(
        "{} business service error: {}", businessService, exception.getMessage(), exception);
  }

  /**
   * Advice that alerts a system administrator of a problem with an integrated system.
   *
   * @param joinPoint The join point.
   * @param exception The exception to log.
   */
  @AfterThrowing(pointcut = "busServices()", throwing = "exception")
  public void notifySystemAdmin(JoinPoint joinPoint, HpcException exception) {
    if (exception.getIntegratedSystem() != null) {
      logger.info("Sending a notification to system admin: " + exception.getMessage());

      // Create a payload containing the exception data.
      List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();

      HpcEventPayloadEntry integratedSystemPayloadEntry = new HpcEventPayloadEntry();
      integratedSystemPayloadEntry.setAttribute("INTEGRATED_SYSTEM");
      integratedSystemPayloadEntry.setValue(exception.getIntegratedSystem().value());
      payloadEntries.add(integratedSystemPayloadEntry);

      HpcEventPayloadEntry errorMessage = new HpcEventPayloadEntry();
      errorMessage.setAttribute("ERROR_MESSAGE");
      errorMessage.setValue(exception.getMessage());
      payloadEntries.add(errorMessage);

      HpcEventPayloadEntry stackTrace = new HpcEventPayloadEntry();
      stackTrace.setAttribute("STACK_TRACE");
      stackTrace.setValue(exception.getStackTraceString());
      payloadEntries.add(stackTrace);

      // Send the notification.
      notificationService.sendNotification(
          systemAdministratorUserId,
          HpcSystemAdminNotificationType.INTEGRATED_SYSTEM_ERROR,
          payloadEntries,
          HpcNotificationDeliveryMethod.EMAIL);
    }
  }

  /**
   * Advice that set up the system account as the request invoker
   *
   * @param joinPoint The join point.
   * @return The advised object return.
   * @throws Throwable The advised object exception.
   */
  @Around("busServices() && @annotation(gov.nih.nci.hpc.bus.aspect.SystemBusServiceImpl)")
  public Object setSystemRequestInvoker(ProceedingJoinPoint joinPoint) throws Throwable {
    logger.info("ERAN: set system request invoker");

    try {
      return joinPoint.proceed();

    } finally {
      logger.info("ERAN: unset system request invoker");
    }
  }
}
