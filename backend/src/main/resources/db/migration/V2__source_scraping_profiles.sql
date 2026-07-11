alter table sources
    add column article_url_patterns text,
    add column body_selectors text,
    add column title_selectors text,
    add column published_at_selectors text;
