package org.javaup.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.Result;
import org.javaup.exception.ArgumentError;
import org.javaup.exception.HmdpFrameException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(value = HmdpFrameException.class)
    public Result<String> toolkitExceptionHandler(HttpServletRequest request, HmdpFrameException hmdpFrameException) {
        log.error("业务异常 错误信息 : {} method : {} url : {} query : {} ", hmdpFrameException.getMessage(), request.getMethod(), getRequestUrl(request), getRequestQuery(request), hmdpFrameException);
        return Result.fail( hmdpFrameException.getMessage());
    }

    @SneakyThrows
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<List<ArgumentError>> validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        log.error("参数验证异常 错误信息 : {} method : {} url : {} query : {} ", ex.getMessage(), request.getMethod(), getRequestUrl(request), getRequestQuery(request), ex);
        BindingResult bindingResult = ex.getBindingResult();
        List<ArgumentError> argumentErrorList =
                bindingResult.getFieldErrors()
                        .stream()
                        .map(fieldError -> {
                            ArgumentError argumentError = new ArgumentError();
                            argumentError.setArgumentName(fieldError.getField());
                            argumentError.setMessage(fieldError.getDefaultMessage());
                            return argumentError;
                        }).collect(Collectors.toList());
        return Result.fail(argumentErrorList);
    }

    @ExceptionHandler(value = Throwable.class)
    public Result<String> defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("全局异常 错误信息 : {} method : {} url : {} query : {} ", throwable.getMessage(), request.getMethod(), getRequestUrl(request), getRequestQuery(request), throwable);
        return Result.fail();
    }

    private String getRequestUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    private String getRequestQuery(HttpServletRequest request){
        return request.getQueryString();
    }
}
