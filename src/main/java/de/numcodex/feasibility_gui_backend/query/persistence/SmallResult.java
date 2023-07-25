package de.numcodex.feasibility_gui_backend.query.persistence;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "result")
public class SmallResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_id", updatable = false, nullable = false)
    private Long queryId;

    @Column(name = "site_id", updatable = false, nullable = false)
    private Long siteId;

    private Integer result;

}
