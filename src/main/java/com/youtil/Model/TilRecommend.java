package com.youtil.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "til_recommends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TilRecommend extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tils_id", nullable = false)
    private Til til;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


}
