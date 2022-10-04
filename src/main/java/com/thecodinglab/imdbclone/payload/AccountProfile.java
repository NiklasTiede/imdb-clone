package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.entity.Role;

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
