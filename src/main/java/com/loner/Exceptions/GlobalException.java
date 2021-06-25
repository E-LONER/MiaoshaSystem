package com.loner.Exceptions;

import com.loner.result.CodeMsg;
import org.omg.SendingContext.RunTime;

//业务异常
public class GlobalException extends RuntimeException {

    private CodeMsg cm;

    public GlobalException(CodeMsg cm){
        this.cm=cm;
    }

    public CodeMsg getCm() {
        return cm;
    }
}
