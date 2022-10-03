package com.example.demo.payload;

import com.example.demo.entity.Role;
import java.util.Collection;
import java.util.Date;

public record AccountProfile(

        String username,
        String email,
        String password,
        String firstName,
        String lastName,
        String phone,
        String bio,
        Date birthday,
        Collection<Role> roles,
        Long ratingsCount,
        Long watchlistCount,
        Long commentsCount
) {}
