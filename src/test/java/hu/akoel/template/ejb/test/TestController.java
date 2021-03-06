package hu.akoel.template.ejb.test;

import hu.akoel.tamplate.service.ContextService;
import hu.akoel.template.ejb.services.JsonService;
import hu.akoel.template.ejb.test.annotation.TestInputSet;
import hu.akoel.template.ejb.test.exception.TestCompareXMLToDBException;
import hu.akoel.template.ejb.test.exception.TestCompareXMLToDBFormatException;
import hu.akoel.template.ejb.test.exception.TestException;
import hu.akoel.template.ejb.test.exception.TestNoExceptionException;
import hu.akoel.template.ejb.test.exception.TestNotExpectedExceptionException;
import hu.akoel.template.ejb.test.exception.TestNotExpectedExceptionMessageException;
import hu.akoel.template.spring.HelloWorld;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;

//import org.glassfish.jersey.message.internal.HeaderValueException.Context;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xml.sax.SAXException;

public class TestController{
	protected ArrayList<Liquibase> liquibaseList = new ArrayList<>();
	private Connection conn = null;
	private Database database;	
	
	private static ContextService context;
	
	@Rule
	public TestWatcher testWatcher = new MyTestWatcher();

	public TestController(){
System.err.println("============= Nyitas =================");	
		context = new ContextService( ContextService.EJBTYPE.SERVER );

		//----------------------
		// Gain the database
		//----------------------
		DataSource ds;					
		try {
			ds = context.getDatabase();
			conn = ds.getConnection();
			database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
		} catch (NamingException | DatabaseException | SQLException e) {
			//e.printStackTrace();
			throw new Error(e);				
		}
	}
	
	@AfterClass
	public static void close() throws NamingException{
System.err.println("============= Zaras =================");		
		context.close();
	}
	
	public ContextService getContext(){
		return context;
	}
	
	class MyTestWatcher extends TestWatcher {		

		@Override
		protected void starting(final Description description) {
			
			//----------------------
			// Clear the cache
			//----------------------			
			try {
				context.clearCache();
			} catch (NamingException e) {
				e.printStackTrace();
				throw new Error(e);
			}			
			
			//--------------------
			// Gain liquidbaseList
			//--------------------
			TestInputSet dataSetAnnnotation = (TestInputSet)description.getAnnotation(TestInputSet.class);
			if( null != dataSetAnnnotation ){
				for( String value: dataSetAnnnotation.value() ){
					//databaseChangeLogXmlList.add( value );
					
					try {					
						Liquibase liquibase = new Liquibase(value, new FileSystemResourceAccessor(), database);
						liquibaseList.add( liquibase );
					} catch (LiquibaseException e) {
						e.printStackTrace();
						throw new Error(e);
					}
				}
			}

			//----------------------
			// Clears all tables
			// Fill out tables
			//----------------------
			for( Liquibase liquibase : liquibaseList ){
				try {					
					liquibase.update("");
				} catch (LiquibaseException e) {
					e.printStackTrace();
					throw new Error(e);
				}
			}
		}

		@Override
		protected void finished(final Description description) {
			super.finished(description);

			try {
				for( Liquibase liquibase: liquibaseList) {
					liquibase.rollback(1000, null);
				}				
			} catch (Exception e) {
				e.printStackTrace();
				throw new Error(e);
			}finally{
				try{
					conn.close();
				}catch(Exception f){}
			}
		}
	};

	protected TestControllerChainParameterHelper initializeSession( Object service, String methodName, Object[] parameterList ){
		return new TestControllerChainParameterHelper( this, service, methodName, parameterList );
	}

	/**
	 * Invokes Session operation
	 * 
	 * @param session
	 * @param sessionMethodName
	 * @param parameterList
	 * @param expectedJSONObjectFileName
	 * @param expectedJSONArrayFileName
	 * @param expectedXMLFileName
	 * @param expectedException
	 * @return
	 * @throws TestException
	 * @throws JSONException 
	 * @throws IOException 
	 */
	//
	@SuppressWarnings("unchecked")
	public <E> E doSession( Object session, String sessionMethodName, Object[] parameterList, String expectedJSONObjectFileName, String expectedJSONArrayFileName, String expectedXMLFileName, ExpectedExceptionObject expectedException ) throws TestException, IOException, JSONException{

		E actualResult = null;
		
		Class<?>[] parameterClassList = new Class<?>[ parameterList.length ];
		for( int i = 0; i < parameterList.length; i++ ){
			parameterClassList[i] = parameterList[i].getClass();
		}
		
		Method method = null;
		try {
			method = session.getClass().getMethod(sessionMethodName, parameterClassList );			
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new Error(e);
		}

		try {
			  actualResult = (E) method.invoke(session, parameterList);
			  System.out.println("--- Result ---");
			  System.out.println( JsonService.getJsonStringFromJavaObject( actualResult ) );
			  System.out.println("--------------");
			  if( null != expectedException ){
				  throw new TestNoExceptionException( expectedException.getExpectedClass().toString() );
			  }
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new Error(e);
		} catch(InvocationTargetException e){
			
			Throwable targetException = e.getTargetException();
			
			//There was an exception but was not defined the expected OR it was not the expected !!!!
			if( null == expectedException || !targetException.getClass().equals( expectedException.getExpectedClass()) ){				
				throw new TestNotExpectedExceptionException( expectedException.getClass().getSimpleName(), targetException.getClass().getSimpleName() );
			
			//The exception was the expected but probably the message was different	
			}else{
				
				String expectedMessage = expectedException.getExactMessage();

				//The exact message was specified but it not the same as the catched
				if( null != expectedMessage && !expectedMessage.equals( targetException.getMessage() ) ){
					throw new TestNotExpectedExceptionMessageException( expectedMessage, targetException.getLocalizedMessage() );
				}
				
			}
		}
		
		//--------------------
		// Compare XML the DB
		//--------------------
		if( null != expectedXMLFileName ){
			try {
				String difference = CompareXMLToDB.getDifference(expectedXMLFileName, conn, database);
				if( null != difference ){
					throw new TestCompareXMLToDBException(expectedXMLFileName, difference );
				}
			} catch (ParserConfigurationException |SAXException |IOException | SQLException e) {				
				//e.printStackTrace();
				throw new TestCompareXMLToDBFormatException(e.getLocalizedMessage());
			}
		}
		
		//---------------------------------
		// Compare JSONObject to the result
		//---------------------------------			
		if( null != expectedJSONObjectFileName ){
			CompareJSONToResult.doCompareAsJSONObject(expectedJSONObjectFileName, actualResult);
		
		//---------------------------------
		// Compare JSONArray to the result
		//---------------------------------	
		}else if( null != expectedJSONArrayFileName ){
			CompareJSONToResult.doCompareAsJSONArray(expectedJSONArrayFileName, actualResult);
		}

		return actualResult;
	}
}
