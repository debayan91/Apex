package io.heron.market;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LatestPriceRepository extends CrudRepository<LatestPrice, String> {
}
