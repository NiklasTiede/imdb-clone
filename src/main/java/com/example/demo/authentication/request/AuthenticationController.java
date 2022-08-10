// package com.example.demo.controller;
//
// import com.example.demo.exceptions.NotFoundException;
// import com.example.demo.request.AuthenticationRequest;
// import com.example.demo.response.JWTTokenResponse;
// import com.example.demo.service.AuthenticationService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
//
// import java.util.logging.Level;
// import java.util.logging.Logger;
//
// @RestController
// public class AuthenticationController {
//
//    private static final Logger logger =
// Logger.getLogger(String.valueOf(AuthenticationController.class));
//
//    @Autowired
//    private AuthenticationService authenticationService;
//
//    @PostMapping("/login")
//    public ResponseEntity<JWTTokenResponse> createCustomer(@RequestBody AuthenticationRequest
// request) {
//        System.out.println();
//        System.out.println(authenticationService.generateJWTToken(request.getUsername(),
// request.getPassword()));
//        logger.log(Level.INFO, "JWT",
// authenticationService.generateJWTToken(request.getUsername(), request.getPassword()));
//        return new ResponseEntity<>(authenticationService.generateJWTToken(request.getUsername(),
// request.getPassword()), HttpStatus.OK);
//    }
//
//    @ExceptionHandler(NotFoundException.class)
//    public ResponseEntity<String> handleEntityNotFoundException(NotFoundException ex) {
//        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
//    }
// }
