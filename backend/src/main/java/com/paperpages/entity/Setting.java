package com.paperpages.entity;

import jakarta.persistence.*;

/** 站点设置（键值对），目前用于存放「关于」页文字。 */
@Entity
@Table(name = "site_settings")
public class Setting {

    /** 列名用 skey，避免 MySQL 保留字 key */
    @Id
    @Column(name = "skey", nullable = false, length = 50)
    private String key;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
