package com.whut.training.aspect.logging;

import com.whut.training.aspect.annotation.ServiceLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 服务层调用日志切面。
 *
 * <p>该切面仅作用于 {@code com.whut.training.service..*} 包下且被 {@link ServiceLog} 标记的类或方法，
 * 记录请求来源、参数摘要、执行耗时以及异常类型。敏感字段如 password、token、secret 会被脱敏。
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "app.logging.service", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);
    private static final int MAX_LOG_STRING_LENGTH = 120;
    private static final int MAX_FIELDS = 10;

    /**
     * 定义服务包内的切点。
     */
    @Pointcut("within(com.whut.training.service..*)")
    public void inServicePackage() {
    }

    /**
     * 定义类级别的日志标记切点。
     */
    @Pointcut("@within(com.whut.training.aspect.annotation.ServiceLog)")
    public void serviceLogClass() {
    }

    /**
     * 定义方法级别的日志标记切点。
     */
    @Pointcut("@annotation(com.whut.training.aspect.annotation.ServiceLog)")
    public void serviceLogMethod() {
    }

    /**
     * 环绕记录一次服务调用。
     *
     * @param joinPoint 当前切点。
     * @return 目标方法执行结果。
     * @throws Throwable 目标方法抛出的异常会原样抛出。
     */
    @Around("inServicePackage() && (serviceLogClass() || serviceLogMethod())")
    public Object logServiceInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNs = System.nanoTime();
        String requestInfo = buildRequestInfo();
        String className = joinPoint.getTarget() == null
                ? "UnknownService"
                : joinPoint.getTarget().getClass().getSimpleName();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String argsInfo = buildArgsInfo(joinPoint.getArgs());

        logger.info("service_start service={}.{} request={} args={}", className, methodName, requestInfo, argsInfo);

        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("service_end service={}.{} request={} durationMs={}", className, methodName, requestInfo, durationMs);
            return result;
        } catch (Exception ex) {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("service_error service={}.{} request={} durationMs={} exception={}",
                    className, methodName, requestInfo, durationMs, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /**
     * 构造当前请求的简要说明。
     *
     * @return 请求方法、路径和客户端 IP；不在 HTTP 请求上下文时返回 N/A。
     */
    private String buildRequestInfo() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return "N/A";
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String query = request.getQueryString();
        String path = query == null || query.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + query;
        return request.getMethod() + " " + path + " ip=" + request.getRemoteAddr();
    }

    /**
     * 构造参数摘要。
     *
     * @param args 方法参数。
     * @return 参数摘要字符串。
     */
    private String buildArgsInfo(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<String> items = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            items.add("arg" + i + "=" + summarizeValue("arg" + i, args[i]));
        }
        return "[" + String.join(", ", items) + "]";
    }

    /**
     * 对任意值生成日志摘要。
     *
     * @param key   参数名或字段名。
     * @param value 参数值。
     * @return 摘要字符串。
     */
    private String summarizeValue(String key, Object value) {
        if (value == null) {
            return "null";
        }
        if (isSensitiveKey(key)) {
            return "***";
        }
        if (value instanceof String stringValue) {
            return "String(len=" + stringValue.length() + ")";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (value instanceof Map<?, ?> map) {
            return value.getClass().getSimpleName() + "(size=" + map.size() + ")";
        }
        if (value.getClass().getName().startsWith("com.whut.training.")) {
            return summarizeObjectFields(value);
        }
        return value.getClass().getSimpleName();
    }

    /**
     * 摘要输出对象字段。
     *
     * @param value 目标对象。
     * @return 只包含少量字段的对象摘要。
     */
    private String summarizeObjectFields(Object value) {
        Field[] fields = value.getClass().getDeclaredFields();
        if (fields.length == 0) {
            return value.getClass().getSimpleName();
        }

        List<String> items = new ArrayList<>();
        int count = 0;
        for (Field field : fields) {
            if (count >= MAX_FIELDS) {
                items.add("...");
                break;
            }
            field.setAccessible(true);
            Object fieldValue;
            try {
                fieldValue = field.get(value);
            } catch (IllegalAccessException e) {
                fieldValue = "N/A";
            }
            String summarized = summarizeFieldValue(field.getName(), fieldValue);
            items.add(field.getName() + "=" + summarized);
            count++;
        }
        return value.getClass().getSimpleName() + "{" + String.join(", ", items) + "}";
    }

    /**
     * 摘要单个字段值。
     *
     * @param fieldName 字段名。
     * @param value     字段值。
     * @return 适合写入日志的字段摘要。
     */
    private String summarizeFieldValue(String fieldName, Object value) {
        if (isSensitiveKey(fieldName)) {
            return "***";
        }
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            String sanitized = text.replace("\r", "").replace("\n", "");
            if (sanitized.length() > MAX_LOG_STRING_LENGTH) {
                return "\"" + sanitized.substring(0, MAX_LOG_STRING_LENGTH) + "...\"";
            }
            return "\"" + sanitized + "\"";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        return value.getClass().getSimpleName();
    }

    /**
     * 判断字段名是否应脱敏。
     *
     * @param key 字段名。
     * @return 如果包含敏感关键字则返回 true。
     */
    private boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password")
                || lower.contains("token")
                || lower.contains("secret");
    }
}
