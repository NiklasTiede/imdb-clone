// package com.example.demo.service;
//
// import com.example.demo.exceptions.NotFoundException;
// import com.example.demo.repository.UserRepository;
// import com.example.demo.response.JWTTokenResponse;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.stereotype.Component;
//
// @Component
// public class AuthenticationService {
//
//    private final UserRepository userRepository;
//    private final JwtTokenService jwtTokenService;
//
//    public AuthenticationService(UserRepository userRepository, JwtTokenService jwtTokenService) {
//        this.userRepository = userRepository;
//        this.jwtTokenService = jwtTokenService;
//    }
//
//    public JWTTokenResponse generateJWTToken(String username, String password) {
//        BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder();
//        return userRepository.findOneByUsername(username)
//                .filter(account ->  bCrypt.matches(password, account.getPassword()))
//                .map(account -> new JWTTokenResponse(jwtTokenService.generateToken(username)))
//                .orElseThrow(() ->  new NotFoundException("Account not found"));
//    }
// }
