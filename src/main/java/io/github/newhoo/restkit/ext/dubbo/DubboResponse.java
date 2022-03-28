package io.github.newhoo.restkit.ext.dubbo;

import io.github.newhoo.restkit.common.Response;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * dubbo response
 *
 * @author newhoo
 * @date 2022/3/13 8:06 PM
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DubboResponse extends Response {

    private Map<String, String> attachments;

}
