package gov.nist.hit.gvt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import gov.nist.hit.core.domain.UserCFTestInstance;

@Repository
public interface UserCFTestInstanceRepository extends JpaRepository<UserCFTestInstance, Long> {

	@Modifying
	@Query("delete UserCFTestInstance  where id = :id")
	public int deleteCFTestInstance(@Param("id") Long id);
	
}
