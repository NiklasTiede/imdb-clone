package com.thecodinglab.imdbclone.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface CustomUserDetailsService {

  UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException;

  UserDetails loadUserById(Long id);
}
