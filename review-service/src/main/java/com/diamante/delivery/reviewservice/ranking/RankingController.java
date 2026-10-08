package com.diamante.delivery.reviewservice.ranking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.diamante.delivery.reviewservice.ReviewSummary;
import com.diamante.delivery.reviewservice.ReviewSummaryRepository;

@RestController
@RequestMapping("/reviews")
public class RankingController {

    private final ReviewSummaryRepository repository;

    public RankingController(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    /** Reads from the database (not from the buffer): the ranking lags up to one flush interval. */
    @GetMapping("/ranking")
    public List<RankingItem> ranking() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingDouble(ReviewSummary::average).reversed()
                        .thenComparing(Comparator.comparingLong(ReviewSummary::getReviewCount).reversed()))
                .map(summary -> new RankingItem(
                        summary.getDishId(),
                        summary.getDishName(),
                        BigDecimal.valueOf(summary.average()).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                        summary.getReviewCount()))
                .toList();
    }
}
