package com.chat_server.userblock.exception;

public class UserBlockExistsException extends RuntimeException{
    public UserBlockExistsException(){
        super("user block exist");
    }

    public UserBlockExistsException(String message){
        super(message);
    }
}
