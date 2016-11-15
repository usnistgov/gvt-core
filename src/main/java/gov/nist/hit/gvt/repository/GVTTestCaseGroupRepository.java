package gov.nist.hit.gvt.repository;

import java.util.List;

import gov.nist.hit.gvt.domain.GVTTestCaseGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GVTTestCaseGroupRepository extends JpaRepository<GVTTestCaseGroup, Long> {

	public List<GVTTestCaseGroup> findByPreloaded(boolean preloaded);
	
	@Query("select tcg from GVTTestCaseGroup tcg where tcg.userId = :userId and preloaded = false")
	public List<GVTTestCaseGroup> userExclusive(@Param("userId") Long id);
}
