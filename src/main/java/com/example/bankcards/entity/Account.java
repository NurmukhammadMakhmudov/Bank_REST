package com.example.bankcards.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@ToString
public class Account {

    @Id
    private long id;

    @Column(name = "created_at")
    private LocalDate createdAt;

}
