package com.example.demo.repository;

import com.example.demo.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Integer> {}
