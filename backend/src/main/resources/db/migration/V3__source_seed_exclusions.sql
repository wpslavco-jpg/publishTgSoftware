create table source_seed_exclusions (
    code varchar(64) primary key,
    excluded_at timestamp with time zone not null
);
