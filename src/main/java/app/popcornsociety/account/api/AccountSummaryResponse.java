package app.popcornsociety.account.api;

public record AccountSummaryResponse(
    Long id, String username, String email, String firstName, String lastName) {}
