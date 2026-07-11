package com.thecodinglab.imdbclone.recommendation.api;

import java.util.List;

public record HomeFeedSection(
    String id, String title, String subtitle, String family, List<HomeFeedItem> items) {}
