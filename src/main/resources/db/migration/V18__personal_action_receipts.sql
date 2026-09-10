-- Preserve existing add receipts while extending the same retry ledger to personal changes.
alter table watchlist_action_receipt rename to engagement_action_receipt;
alter table engagement_action_receipt rename column created to changed;
alter table engagement_action_receipt rename column added_at to occurred_at;
alter table engagement_action_receipt add column kind varchar(32) not null default 'watchlist_add';
alter table engagement_action_receipt alter column kind drop default;
alter table engagement_action_receipt add column score numeric(3,1);
alter table engagement_action_receipt add column previous_score numeric(3,1);
alter table engagement_action_receipt add constraint engagement_receipt_kind check
    (kind in ('watchlist_add', 'watchlist_remove', 'rating_set', 'rating_remove'));
alter table engagement_action_receipt add constraint engagement_receipt_score check
    ((kind = 'rating_set' and score between 0 and 10 and score is not null)
     or (kind <> 'rating_set' and score is null));
alter table engagement_action_receipt add constraint engagement_receipt_previous_score check
    (previous_score is null or (kind in ('rating_set', 'rating_remove') and previous_score between 0 and 10));
alter index idx_watchlist_action_receipt_retention rename to idx_engagement_action_receipt_retention;
