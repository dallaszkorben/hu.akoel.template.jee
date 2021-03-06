package hu.akoel.template.ejb.entities;

import hu.akoel.template.ejb.services.JsonService;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="jsonId")
@Entity
@Table(
		name="role",  
		uniqueConstraints=@UniqueConstraint(columnNames={"name", "active"}))
public class Role extends HistoryMappedSuperclass<Role> implements EntityObject{

	private static final long serialVersionUID = -8046466070317090805L;
	
	private Integer id;
	private String name;
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="role_id_seq_gen")
	@SequenceGenerator(allocationSize=1,initialValue=1,name="role_id_seq_gen", sequenceName="role_id_seq")
	@Column(name="id")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(nullable=false, unique=false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public int hashCode(){
		final int prime = 31;
		int hash = 1;
		hash = hash * prime + this.getId().hashCode();
		return hash;
	}
	
	public boolean equals( final Object otherObject ) {		
        if (otherObject instanceof Role) {
            final Role other = (Role) otherObject;
            return 
            		other == this ||
            		other.getId().equals( this.getId() );
        }	 
        return false;
	}
	
/*	@Override
	public boolean equalsByThisNotNullFields(Object otherObject) {
		if( null != otherObject && otherObject instanceof Role ){
			Role other = (Role)otherObject;
			if( 
					( null == this.id || this.id.equals(other.id) ) && 
					( null == this.getName() || this.getName().equals( other.getName() ) ) 
			){
				return true;
			}			
		}
		return false;
	}
*/	
	public String toString(){
		//StringBuffer out = new StringBuffer();
		//out.append("Role = id: " + this.getId() );
		//out.append(", Name: " + this.getName() );
		//out.append(", Ch: " + DateService.getTimeByDefaultFormatter( this.getCapturedAt().getTime() ) );
		//out.append(", By: " + (this.getCapturedBy() == null ? "": this.getCapturedBy().getName()));
		//return out.toString();
		
		return JsonService.getJsonStringFromJavaObject(this);
	}

}
