package com.letv.mas.router.config;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by leeco on 18/8/7.
 */
@ConditionalOnProperty(value = "zuul.debug.logfile", havingValue = "true", matchIfMissing = false)
@Component
public class ZuulLogFilter extends ZuulFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulLogFilter.class);

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE; // 在routing和error过滤器之后调用
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        try {
            RequestContext requestContext = RequestContext.getCurrentContext();
            final List<String> routingDebug = (List<String>) requestContext.get("routingDebug");
            final Map<String, String> routingMap = this.parseRoutingDebug(routingDebug);
            HttpServletRequest request = requestContext.getRequest();

/*            // 打印请求参数
            InputStream in = request.getInputStream();
            String reqBbody = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
            LOGGER.info("request body: {}", reqBbody);
            if (reqBbody != null) {
                JSONObject json = JSONObject.parseObject(reqBbody);
                Object obj = json.get("xxxx");
                if (obj != null) {
                    LOGGER.info("request xxxx: {}" + obj);
                }
            }*/

            // 按现服役nginx格式打印
            StringBuilder sb = new StringBuilder();
            String splitSymbol = "|";
//            float upstream_response_time = 0.0f, request_time = 0.0f;
            int upstream_response_time = 0, request_time = 0;
//            int unit = 1000;
            if (routingMap.size() > 0) {
                try {
//                    upstream_response_time = (new BigDecimal(routingMap.get("postTime"))).divide(new BigDecimal(unit), 3, BigDecimal.ROUND_HALF_UP).floatValue();
//                    request_time = upstream_response_time + (new BigDecimal(routingMap.get("preTime"))).divide(new BigDecimal(unit), 3, BigDecimal.ROUND_HALF_UP).floatValue();
//                    request_time = (float) (Math.round(request_time * unit)) / unit;
                    upstream_response_time = Integer.parseInt(routingMap.get("postTime"));
                    request_time = upstream_response_time + Integer.parseInt(routingMap.get("preTime"));
                } catch (NumberFormatException nfe) {

                }
            }

            /*
                 log_format  '$remote_addr^A$http_x_forwarded_for^A$remote_user^A$time_iso8601^A'
                             '$request_method^A$scheme^A$host^A$request_uri^A$server_protocol^A'
                             '$status^A$request_length^A$bytes_sent^A$body_bytes_sent^A'
                             '$http_referer^A$http_user_agent^A$upstream_addr^A$upstream_response_time^A
            */
            sb.append(this.getISO8601Timestamp(new Date())).append(splitSymbol)
                    .append(request.getRemoteAddr()).append(splitSymbol)
                    .append(this.handleEmpty(request.getHeader("x-forwarded-for"))).append(splitSymbol)
                    .append(this.handleEmpty(request.getRemoteUser())).append(splitSymbol)
                    .append(request.getMethod()).append(splitSymbol)
                    .append(request.getScheme()).append(splitSymbol)
                    .append(this.handleEmpty(request.getRemoteHost())).append(splitSymbol)
                    .append(request.getRequestURI()).append(splitSymbol)
                    .append(request.getProtocol()).append(splitSymbol)
                    .append(requestContext.getResponseStatusCode()).append(splitSymbol)
                    .append(this.handleEmpty(routingMap.get("contentLength"))).append(splitSymbol)
//                    .append(this.handleEmpty(routingMap.get("contentLength"))).append(splitSymbol) //TODO
//                    .append(this.handleEmpty(routingMap.get("contentLength"))).append(splitSymbol) //TODO
                    .append(this.handleEmpty(request.getHeader("referer"))).append(splitSymbol)
                    .append(this.handleEmpty(request.getHeader("user-agent"))).append(splitSymbol)
                    .append(this.handleEmpty(routingMap.get("routeHost"))).append(splitSymbol)
                    .append(upstream_response_time).append(splitSymbol)
                    .append(request_time)
            ;
            LOGGER.info(sb.toString());

/*            LOGGER.info("request url: {}", request.getRequestURL().toString());
            Map<String, String[]> map = request.getParameterMap();
            // 打印请求url参数
            if (map != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("request parameters:\t");
                for (Map.Entry<String, String[]> entry : map.entrySet()) {
                    sb.append("[").append(entry.getKey()).append("=").append(printArray(entry.getValue())).append("]");
                }
                LOGGER.info(sb.toString());
            }*/

/*            // 打印响应数据集合
            InputStream out = requestContext.getResponseDataStream();
            String outBody = StreamUtils.copyToString(out, Charset.forName("UTF-8"));
            if (outBody != null) {
                LOGGER.info("response body: {}", outBody);
            }
            requestContext.setResponseBody(outBody); // 输出流被读取后需归还！*/
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String printArray(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * 传入Data类型日期，返回字符串类型时间（ISO8601标准时间）
     *
     * @param date
     * @return
     */
    private String getISO8601Timestamp(Date date) {
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(tz);
        String nowAsISO = df.format(date);
        return nowAsISO;
    }

    private String handleEmpty(String str) {
        if (StringUtils.isEmpty(str)) {
            return "-";
        } else {
            return str;
        }
    }

    private Map<String, String> parseRoutingDebug(List<String> debugInfos) {
        HashMap<String, String> ret = new HashMap<String, String>();
        int index = -1;
        String symbol = null;
        boolean found = false;

        for (String debugInfo : debugInfos) {
            index = -1;
            found = false;

            symbol = "routeHost";
            if (debugInfo.contains(symbol + "=")) {
                index = debugInfo.indexOf(symbol);
                ret.put(symbol, debugInfo.substring(index + symbol.length() + 1));
                found = true;
            }

            symbol = "time = ";
            if (!found && debugInfo.contains("PreDecorationFilter") && debugInfo.contains(symbol)) {
                index = debugInfo.indexOf(symbol);
                ret.put("preTime", debugInfo.substring(index + symbol.length(), debugInfo.length() - 2));
                found = true;
            }

            symbol = "time = ";
            if (!found && debugInfo.contains("SimpleHostRoutingFilter") && debugInfo.contains(symbol)) {
                index = debugInfo.indexOf(symbol);
                ret.put("postTime", debugInfo.substring(index + symbol.length(), debugInfo.length() - 2));
                found = true;
            }

            symbol = "originContentLength=";
            if (!found && debugInfo.contains("SimpleHostRoutingFilter") && debugInfo.contains(symbol)) {
                index = debugInfo.indexOf(symbol);
                ret.put("contentLength", debugInfo.substring(index + symbol.length()));
                found = true;
            }
        }
        return ret;
    }
}