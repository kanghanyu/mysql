//package com.khy.config.compensable.utils;
//
//
//import java.io.UnsupportedEncodingException;
//import java.lang.reflect.Type;
//import java.util.Date;
//
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.client.RestTemplate;
//
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.fastjson.TypeReference;
//import com.alibaba.fastjson.parser.Feature;
//import com.alibaba.fastjson.serializer.SerializerFeature;
//import com.khy.common.JsonResponse;
//import com.khy.config.compensable.SpringBootBeanRegistry;
//
//public final class CompensableServiceUtils {
////    static Logger logger = SearchableLoggerFactory.getDefaultLogger();
//	  static final Logger logger = LoggerFactory.getLogger(CompensableServiceUtils.class);
//    private static final CompensableServiceUtils instance = new CompensableServiceUtils();
//    @Autowired
//    private RestTemplate restTemplate;
//
//    private CompensableServiceUtils() {
//    }
//
//    public static CompensableServiceUtils getInstance() {
//        return instance;
//    }
//
//    public static String invokePost(String url) throws RuntimeException {
//        return invokePost(url, (String)null);
//    }
//
//    public static String invokePost(String url, String text) throws RuntimeException {
//        return invokePost(url, text, "application/json; charset=UTF-8");
//    }
//
//    public static String invokePost(String url, String text, String contentType) throws RuntimeException {
//        try {
//            RestTemplate restTemplate = getInstance().getRestTemplate();
//            HttpEntity<String> requestEntity = null;
//            if (StringUtils.isNotBlank(text)) {
//                HttpHeaders headers = new HttpHeaders();
//                headers.add("Content-Type", contentType);
//                addJwtTokenRequestHeader(headers);
//                requestEntity = new HttpEntity(text, headers);
//            }
//
//            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class, new Object[0]);
//            int statusCode = responseEntity.getStatusCodeValue();
//            if (statusCode >= 400 && statusCode < 500) {
//                throw new RuntimeException(String.format("POST请求出错(status= %s)!", statusCode));
//            } else if (statusCode >= 500) {
//                throw new RuntimeException(String.format("POST请求出错(status= %s)!", statusCode));
//            } else {
//                return (String)responseEntity.getBody();
//            }
//        } catch (RuntimeException var7) {
//            logger.error("POST请求(url= {}, text= {})出错!", new Object[]{url, text, var7});
//            throw var7;
//        } catch (Exception var8) {
//            logger.error("POST请求(url= {}, text= {})出错!", new Object[]{url, text, var8});
//            throw new RuntimeException("POST请求出错!", var8);
//        }
//    }
//
//    public static <T> T invokePost(String url, Class<T> clazz) throws RuntimeException {
//        checkType(url, (String)null, true, clazz);
//        return invokePost(url, (String)null, clazz);
//    }
//
//    public static <T> T invokePost(String url, String text, Class<T> clazz) throws RuntimeException {
//        checkType(url, text, true, clazz);
//        String response = invokePost(url, text);
//        return JSONObject.parseObject(response, clazz);
//    }
//
//    public static <T> JsonResponse<T> invokePost(String url, JsonRequest<?> request) {
//        if (request.getReqBody() != null) {
//            checkType(url, JSONObject.toJSONString(request), true, request.getReqBody().getClass());
//        }
//
//        String input = JSONObject.toJSONString(request, new SerializerFeature[]{SerializerFeature.DisableCircularReferenceDetect});
//        String value = invokePost(url, input);
//        JsonResponse<T> response = (JsonResponse)JSONObject.parseObject(value, new TypeReference<JsonResponse<T>>() {
//        }, new Feature[0]);
//        if (response.getRspBody() != null) {
//            checkType(url, value, false, response.getRspBody().getClass());
//        }
//
//        return response;
//    }
//
//    public static <T> JsonResponse<T> invokePost(String url, JsonRequest<?> request, Class<T> clazz) {
//        if (request.getReqBody() != null) {
//            checkType(url, JSONObject.toJSONString(request), true, request.getReqBody().getClass());
//        }
//
//        String input = JSONObject.toJSONString(request, new SerializerFeature[]{SerializerFeature.DisableCircularReferenceDetect});
//        String value = invokePost(url, input);
//        JsonResponse<T> response = (JsonResponse)JSONObject.parseObject(value, new TypeReference<JsonResponse<T>>(new Type[]{clazz}) {
//        }, new Feature[0]);
//        if (response.getRspBody() != null) {
//            checkType(url, String.valueOf(value), false, response.getRspBody().getClass());
//        }
//
//        return response;
//    }
//
//    private static boolean checkType(String url, String text, boolean requestFlag, Class<?> clazz) {
//        boolean clazzValid = true;
//        if (String.class.equals(clazz)) {
//            clazzValid = false;
//        } else if (Number.class.isAssignableFrom(clazz)) {
//            clazzValid = false;
//        } else if (Boolean.class.equals(clazz)) {
//            clazzValid = false;
//        } else if (Character.class.equals(clazz)) {
//            clazzValid = false;
//        }
//
//        if (!clazzValid) {
//            if (requestFlag) {
//                throw new IllegalArgumentException(String.format("根据客户端接口报文规范, reqBody不允许是字符串或java原生类型(url= %s, text= %s, clazz= %s)!", url, text, clazz.getName()));
//            } else {
//                throw new IllegalArgumentException(String.format("根据客户端接口报文规范, rspBody不允许是字符串或java原生类型(url= %s, text= %s, clazz= %s)!", url, text, clazz.getName()));
//            }
//        } else {
//            return true;
//        }
//    }
//
//    private static void addJwtTokenRequestHeader(HttpHeaders headers) throws IllegalArgumentException, UnsupportedEncodingException {
//        Algorithm algorithm = Algorithm.HMAC256("201805101547");
//        String token = JWT.create().withIssuer("cn.ygego.ycsg").withExpiresAt(new Date(System.currentTimeMillis() + 30000L)).sign(algorithm);
//        headers.add("X-YGEGO-TOKEN", token);
//    }
//
//    public RestTemplate getRestTemplate() {
//        return this.restTemplate;
//    }
//
//    public void setRestTemplate(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }
