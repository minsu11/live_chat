package com.chat_server.userprofile.exception;

public class UserProfileNotFoundException extends RuntimeException{
    public UserProfileNotFoundException(){
        super("user profile not found");
    }

    public UserProfileNotFoundException(String message){
        super(message);
    }

}
