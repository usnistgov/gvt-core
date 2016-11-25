package gov.nist.hit.gvt.domain;

import gov.nist.hit.core.domain.CFTestInstance;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class GVTTestCaseGroup {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@JsonIgnore
	@NotNull
	private Long userId;
	
	@NotNull
	private String name;
	private String description;
	
	@OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
	@JoinTable(
			name = "gvt_tcg_tc", 
			joinColumns = {
					@JoinColumn(name = "gvt_tcg_id", referencedColumnName = "id")
			},
			inverseJoinColumns = {
				@JoinColumn(name = "gvt_tc_id", referencedColumnName = "id")
			}
	)
	private List<CFTestInstance> testCases;
	
	@JsonIgnore
	@NotNull
	private boolean preloaded;
	
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<CFTestInstance> getTestCases() {
		return testCases;
	}
	public void setTestCases(List<CFTestInstance> testCases) {
		this.testCases = testCases;
	}
	public boolean isPreloaded() {
		return preloaded;
	}
	public void setPreloaded(boolean preloaded) {
		this.preloaded = preloaded;
	}
}
