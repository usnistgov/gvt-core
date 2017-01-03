package gov.nist.hit.gvt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import gov.nist.hit.core.domain.UserTestCaseGroup;

@Repository
public interface UserTestCaseGroupRepository extends JpaRepository<UserTestCaseGroup, Long> {

	public List<UserTestCaseGroup> findByPreloaded(boolean preloaded);
	
	@Query("select tcg from UserTestCaseGroup tcg where tcg.userId = :userId and preloaded = false")
	public List<UserTestCaseGroup> userExclusive(@Param("userId") Long id);
}
