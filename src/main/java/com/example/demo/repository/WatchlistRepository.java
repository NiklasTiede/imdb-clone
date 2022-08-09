package com.example.demo.repository;

import com.example.demo.entity.Watchlist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

  List<Watchlist> findAllByMovieId(Long movieId);

  List<Watchlist> findAllByAccountId(Long accountId);
}
