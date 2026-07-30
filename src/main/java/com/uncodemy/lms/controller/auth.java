package com.uncodemy.lms.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/auth")
public class auth {

@PostMapping("/student/login")
 public String  emailVerify( String Email){

    return " yes verify "  + Email ;
 }

 
}
