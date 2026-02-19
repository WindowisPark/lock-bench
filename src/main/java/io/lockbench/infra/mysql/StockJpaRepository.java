package io.lockbench.infra.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface StockJpaRepository extends JpaRepository<StockEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockEntity s where s.productId = :productId")
    Optional<StockEntity> findByIdForUpdate(@Param("productId") Long productId);

    @Modifying
    @Query("""
            update StockEntity s
            set s.quantity = s.quantity - :quantity
            where s.productId = :productId
              and s.quantity >= :quantity
            """)
    int decrementQuantityNoLock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("""
            update StockEntity s
            set s.quantity = s.quantity - :quantity,
                s.version = s.version + 1
            where s.productId = :productId
              and s.version = :expectedVersion
              and s.quantity >= :quantity
            """)
    int decrementQuantityOptimistic(@Param("productId") Long productId,
                                    @Param("quantity") int quantity,
                                    @Param("expectedVersion") long expectedVersion);
}
