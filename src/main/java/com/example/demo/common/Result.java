package com.example.demo.common;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Result<T> {
    private String code;
    private String msg;
    private T data;
    public Result() {}
    public Result(T data) {this.data = data;}
    public static Result<?> success() {
        Result<Object> result = new Result<>();
        result.setCode("0");
        result.setMsg("success");
        return result;
    }

    public static <T> Result<T> success(T  data) {
        Result<T> result = new Result<>(data);
        result.setCode("0");
        result.setMsg("success");
        return result;
    }

    public static Result error(String code,String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

}