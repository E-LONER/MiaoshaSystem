package com.loner.Exceptions;

import com.loner.log.SystemLog;
import com.loner.result.CodeMsg;
import com.loner.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(value=Exception.class)
    public Result<String> allExceptionHandler(HttpServletRequest request, Exception ex) throws Exception{
        //捕获绑定异常
        if (ex instanceof BindException){
            BindException e= (BindException) ex;
            List<ObjectError> errors=e.getAllErrors();
            ObjectError error=errors.get(0);
            String message=error.getDefaultMessage();
            SystemLog.convException(e.getAllErrors());
            return Result.error(CodeMsg.BIND_Err.fillArgs(message));
            //捕获业务异常
        }else if (ex instanceof GlobalException){
            GlobalException e=(GlobalException)ex;
            return Result.error(e.getCm());
        }else {
            return Result.error(CodeMsg.serverErr);
        }




    }
}