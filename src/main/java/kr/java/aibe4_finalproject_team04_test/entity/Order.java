package kr.java.aibe4_finalproject_team04_test.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private int amount;

    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;

    private String status;
}
