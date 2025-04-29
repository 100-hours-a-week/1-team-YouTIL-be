package com.youtil.Model;

import com.youtil.Common.Enums.Status;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "tils")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Til extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 40)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean isDisplay;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String tag;

    @Column(length = 100, nullable = false)
    private String commitRepository;

    @Column(nullable = false)
    private Boolean isUploaded;

    @Column(nullable = false)
    private Integer recommendCount = 0;

    @Column(nullable = false)
    private Integer visitedCount = 0;

    @Column(nullable = false)
    private Integer commentsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "til", cascade = CascadeType.ALL)
    private List<Interview> interviews;
}
