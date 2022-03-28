package io.github.newhoo.restkit.ext.dubbo;

import io.github.newhoo.restkit.common.Request;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * dubbo request
 *
 * @author newhoo
 * @date 2022/3/13 3:08 PM
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DubboRequest extends Request {

    private String applicationName;
    private String registry;

    private String interfaceName;
    private String methodName;
    private String[] parameterTypes;
    private Object[] parameterValues;
    private String group;
    private String version;
    private Integer timeout;
    private Integer retries;
    private Boolean check;
    private String loadbalance;

    private Map<String, String> attachment;
}
