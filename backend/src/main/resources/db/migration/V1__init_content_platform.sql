create table sources (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(128) not null,
    type varchar(16) not null,
    base_url varchar(512) not null,
    listing_url varchar(512) not null,
    rss_url varchar(512),
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table raw_articles (
    id bigserial primary key,
    source_id bigint not null references sources(id),
    canonical_url varchar(1024) not null unique,
    title varchar(512) not null,
    slug varchar(256) not null,
    content_hash varchar(128) not null,
    status varchar(32) not null,
    published_at timestamp with time zone not null,
    markdown_path varchar(1024) not null,
    raw_excerpt text,
    source_payload text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table prepared_articles (
    id bigserial primary key,
    raw_article_id bigint not null unique references raw_articles(id),
    title varchar(512) not null,
    translated_body text,
    summary_body text,
    editorial_notes text,
    needs_manual_review boolean not null,
    status varchar(32) not null,
    llm_model varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table telegram_configs (
    id bigserial primary key,
    active boolean not null,
    bot_token varchar(512) not null,
    chat_id varchar(128) not null,
    bot_username varchar(128),
    validated boolean not null,
    last_validated_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table publication_jobs (
    id bigserial primary key,
    prepared_article_id bigint not null references prepared_articles(id),
    status varchar(32) not null,
    scheduled_at timestamp with time zone not null,
    attempt_count integer not null,
    telegram_message_id varchar(128),
    last_error text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_raw_articles_published_at on raw_articles (published_at desc);
create index idx_prepared_articles_status on prepared_articles (status);
create index idx_publication_jobs_status_scheduled_at on publication_jobs (status, scheduled_at);
