package io.github.newhoo.restkit.ext.dubbo;

import com.alibaba.dubbo.common.logger.Level;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.project.Project;
import io.github.newhoo.restkit.common.KV;
import io.github.newhoo.restkit.common.Request;
import io.github.newhoo.restkit.common.RequestInfo;
import io.github.newhoo.restkit.common.RestClientData;
import io.github.newhoo.restkit.common.RestItem;
import io.github.newhoo.restkit.restful.RestClient;
import io.github.newhoo.restkit.restful.ep.RestClientProvider;
import io.github.newhoo.restkit.util.JsonUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * dubbo client
 *
 * @author newhoo
 * @date 2022/3/13 2:12 PM
 * @since 1.0.0
 */
public class DubboClient implements RestClient {

    @NotNull
    @Override
    public String getProtocol() {
        return "dubbo";
    }

    @Override
    public List<KV> getConfig(@NotNull RestItem restItem, @NotNull Project project) {
        return Arrays.asList(
                new KV("registry", "{{registry}}"),
                new KV("url", "{{referenceUrl}}"),
                new KV("timeout", "2000")
        );
    }

    @NotNull
    @Override
    public Request createRequest(RestClientData restClientData, Project project) {
        Map<String, String> config = restClientData.getConfig();
        JSONObject jsonObject = JSONObject.parseObject(restClientData.getBody());

        String applicationName = StringUtils.defaultIfEmpty(config.get("applicationName"), "RESTKit-Dubbo-proxy");
        String registry = StringUtils.defaultIfEmpty(config.get("registry"), "zookeeper://127.0.0.1:2181");
        registry = registry.replaceFirst("\\{\\{registry}}", "zookeeper://127.0.0.1:2181");
        String url = StringUtils.defaultString(config.get("url")).replaceFirst("\\{\\{referenceUrl}}", "");//dubbo://127.0.0.1:20880

        String interfaceName = jsonObject.getString("interface");
        String methodName = jsonObject.getString("method");
        String group = StringUtils.defaultString(jsonObject.getString("group"));
        String version = StringUtils.defaultString(jsonObject.getString("version"));

        Integer timeout = (int) Double.parseDouble(ObjectUtils.defaultIfNull(config.get("timeout"), "5000"));
        Integer retries = (int) Double.parseDouble(ObjectUtils.defaultIfNull(config.get("retries"), "0"));
        Boolean check = "true".equals(ObjectUtils.defaultIfNull(config.get("check"), "false"));
        String loadbalance = config.get("loadbalance");

        String[] parameterTypes = jsonObject.getJSONArray("parameterTypes").toArray(new String[0]);
        Object[] parameterValues = jsonObject.getJSONArray("parameterValues").toArray(new Object[0]);

        DubboRequest dubboRequest = new DubboRequest();
        dubboRequest.setApplicationName(applicationName);
        dubboRequest.setRegistry(registry);

        dubboRequest.setInterfaceName(interfaceName);
        dubboRequest.setMethodName(methodName);
        dubboRequest.setParameterTypes(parameterTypes);
        dubboRequest.setParameterValues(parameterValues);
        dubboRequest.setGroup(group);
        dubboRequest.setVersion(version);
        dubboRequest.setTimeout(timeout);
        dubboRequest.setUrl(url);
        dubboRequest.setRetries(retries);
        dubboRequest.setCheck(check);
        dubboRequest.setLoadbalance(loadbalance);
        dubboRequest.setAttachment(restClientData.getHeaders());

        dubboRequest.setMethod(restClientData.getMethod());
        dubboRequest.setConfig(restClientData.getConfig());
        dubboRequest.setHeaders(restClientData.getHeaders());
        dubboRequest.setParams(config);
        dubboRequest.setBody(restClientData.getBody());

        return dubboRequest;
    }

    @NotNull
    @Override
    public RequestInfo sendRequest(Request request, Project project) {
        DubboRequest dubboRequest = (DubboRequest) request;
        return invoke(dubboRequest, project);
    }

    private RequestInfo invoke(DubboRequest request, Project project) {
        long startTs = System.currentTimeMillis();

        ApplicationConfig application = new ApplicationConfig(request.getApplicationName());
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(request.getRegistry());
        registry.setClient("zkclient");
        registry.setTimeout(2000);
        registry.setRegister(false);

        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setApplication(application);
        reference.setRegistry(registry);

        reference.setInterface(request.getInterfaceName());
        reference.setTimeout(request.getTimeout());
        reference.setCheck(request.getCheck());
        reference.setRetries(request.getRetries());
        reference.setProtocol("dubbo");
        reference.setGeneric(true);

        if (StringUtils.isNotEmpty(request.getUrl())) {
            reference.setUrl(request.getUrl());
        }

        try {
            // com.alibaba.dubbo.config.AbstractConfig.checkProperty
            if (StringUtils.isNotEmpty(request.getGroup())) {
                reference.setGroup(request.getGroup());
            }
            if (StringUtils.isNotEmpty(request.getVersion())) {
                reference.setVersion(request.getVersion());
            }
            if (StringUtils.isNotEmpty(request.getLoadbalance())) {
                reference.setLoadbalance(request.getLoadbalance());
            }
        } catch (Exception e) {
            return new RequestInfo(request, e.toString());
        }

        ClassLoader bakLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(reference.getClass().getClassLoader());

        try {
            LoggerFactory.setLevel(Level.OFF);
            GenericService genericService = reference.get();
            if (genericService == null) {
                return new RequestInfo(request, "GenericService不存在: " + request.getInterfaceName());
            }

            RpcContext.getContext().setAttachments(request.getAttachment());
            Object o = genericService.$invoke(request.getMethodName(), request.getParameterTypes(), request.getParameterValues());

            DubboResponse dubboResponse = new DubboResponse();
            dubboResponse.setAttachments(RpcContext.getContext().getAttachments());
            dubboResponse.setBody(JsonUtils.toJson(o));
            RequestInfo requestInfo = new RequestInfo(request, dubboResponse, RpcContext.getContext().getUrl().toFullString(), System.currentTimeMillis() - startTs);
            AbstractRegistryFactory.destroyAll();
            return requestInfo;
        } catch (RpcException e) {
            RequestInfo requestInfo = new RequestInfo(request, "rpc exception code: " + e.getCode() + "\n" + e.getLocalizedMessage());
            if (RpcContext.getContext() != null && RpcContext.getContext().getUrl() != null) {
                requestInfo.setRemoteAddress(RpcContext.getContext().getUrl().toFullString());
                requestInfo.setCost(System.currentTimeMillis() - startTs);
            }
            return requestInfo;
        } catch (Throwable e) {
            RequestInfo requestInfo = new RequestInfo(request, e.toString());
            requestInfo.setCost(System.currentTimeMillis() - startTs);
            return requestInfo;
        } finally {
            Thread.currentThread().setContextClassLoader(bakLoader);
        }
    }

    @NotNull
    @Override
    public String formatResponseInfo(RequestInfo requestInfo) {
        DubboRequest request = (DubboRequest) requestInfo.getRequest();
        DubboResponse response = (DubboResponse) requestInfo.getResponse();

        StringBuilder sb = new StringBuilder();

        String status = "ERROR";
        if (response != null) {
            status = "success";
        }
        sb.append("Status: ").append(status).append("    ").append("Time: ").append(requestInfo.getCost()).append("ms").append("\n")
          .append("Registry: ").append(request.getRegistry()).append("\n")
          .append("Remote address: ").append(requestInfo.getRemoteAddress()).append("\n")
          .append("------------------------------------\n");

        sb.append("req: ").append(JsonUtils.toJson(request)).append("\n");

        if (response != null) {
            sb.append("\n");
            sb.append("resp: ").append(response.getBody()).append("\n");
            sb.append("attachments: ").append(JsonUtils.toJson(response.getAttachments()));
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String formatLogInfo(RequestInfo requestInfo) {
        DubboRequest request = (DubboRequest) requestInfo.getRequest();
        DubboResponse response = (DubboResponse) requestInfo.getResponse();

        StringBuilder sb = new StringBuilder();
        sb.append("############################# ").append(LocalDateTime.now()).append(" #############################").append("\n");
        String status = "ERROR";
        if (response != null) {
            status = "success";
        }
        sb.append("Status: ").append(status).append("    ")
          .append("Time: ").append(requestInfo.getCost()).append("ms").append("    ")
          .append("Remote address: ").append(requestInfo.getRemoteAddress()).append("\n\n");

        if (request != null) {
            sb.append(">>> ").append(request.getUrl()).append("\n");
            String reqHeader = request.getAttachment().entrySet()
                                      .stream().map(entry -> entry.getKey() + ": " + entry.getValue())
                                      .collect(Collectors.joining("\n"));
            if (StringUtils.isNotEmpty(reqHeader)) {
                sb.append(reqHeader).append("\n");
            }
        }
        if (response != null) {
            sb.append("\n");
            sb.append("<<< ").append("\n\n");
            if (response.getAttachments() != null) {
                String respHeader = response.getAttachments().entrySet()
                                            .stream().map(entry -> entry.getKey() + ": " + entry.getValue())
                                            .collect(Collectors.joining("\n"));
                if (StringUtils.isNotEmpty(respHeader)) {
                    sb.append("Attachments:\n").append(respHeader).append("\n");
                }
            }
            if (StringUtils.isNotEmpty(response.getBody())) {
                sb.append("\n").append(response.getBody()).append("\n");
            }
        }
        if (StringUtils.isNotEmpty(requestInfo.getErrMsg())) {
            sb.append("\n");
            sb.append("<<< ERROR").append("\n");
            sb.append(requestInfo.getErrMsg()).append("\n");
        }
        sb.append("\n\n");
        return sb.toString();
    }

    public static class DubboClientProvider implements RestClientProvider {
        @Override
        public RestClient createClient() {
            return new DubboClient();
        }
    }
}
