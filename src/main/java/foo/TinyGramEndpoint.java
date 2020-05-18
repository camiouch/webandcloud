package foo;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.auth.EspAuthenticator;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@Api(name = "myApi",
     version = "v1",
     audiences = "331048952451-9k74vlboc2n4vvh12jkct30fhc2euc85.apps.googleusercontent.com",
  	 clientIds = "331048952451-9k74vlboc2n4vvh12jkct30fhc2euc85.apps.googleusercontent.com",
     namespace =
     @ApiNamespace(
		   ownerDomain = "helloworld.example.com",
		   ownerName = "helloworld.example.com",
		   packagePath = "")
     )

public class TinyGramEndpoint {
	
	//-----------------------USER ENTITY METHODS--------------------------------------------//
	
	     
	    //Checking if user has already added. If no, add him
	    /**
	     * 
	     * @param user
	     * @param up
	     * @return TGuser created entity
	     * @throws UnauthorizedException
	     */
		@ApiMethod(name = "userManager",path="userManager", httpMethod = HttpMethod.GET)
		public Entity userManager(User user, TGUserProfil up) throws UnauthorizedException {
			
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
						
			if (user == null) {
				throw new UnauthorizedException("Invalid credentials");
			}
			
			Key userKey = KeyFactory.createKey("TGUser", user.getEmail());
			
			Entity e;
			try {
				
				e = datastore.get(userKey);
				
			} catch (EntityNotFoundException nef) {
				
				e = new Entity("TGUser", user.getEmail());
				e.setProperty("name", up.name);
				e.setProperty("email", user.getEmail());
				
				String[] parts = user.getEmail().split("@");
				String part1 = parts[0]; 
				e.setProperty("pseudo", "@"+part1);
				e.setProperty("bio", "I'm a human being");
				e.setProperty("profilUrl",up.profilUrl);
				
				HashSet<String> following = new HashSet<String>();
				HashSet<String> followers = new HashSet<String>();
				
				e.setProperty("following", following);
				e.setProperty("followers", followers);
				
				Transaction txn = datastore.beginTransaction();
				datastore.put(e);
				txn.commit();
				
				//To verify if the new entity was really created
				
				try {
					
					e = datastore.get(e.getKey());
					
				} catch (EntityNotFoundException nef2) {
					nef2.printStackTrace();
				}
				
			}
			
		
			
			return e;
		}
		
		
		//-----------------------------------------------------------------------------------------------//
		/**
		 *   
		 * @param user
		 * @param cursorString
		 * @return collection of TGUser entities
		 * @throws UnauthorizedException
		 */
		@ApiMethod(name = "get_users", path="users" ,httpMethod = ApiMethod.HttpMethod.GET)
		public CollectionResponse<Entity> getTGUsers(User user, @Nullable @Named("next") String cursorString)
				throws UnauthorizedException {

			if (user == null) {
				throw new UnauthorizedException("Invalid credentials");
			}
			
			Query q = new Query("TGUser");

			
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			PreparedQuery pq = datastore.prepare(q);

			FetchOptions fetchOptions = FetchOptions.Builder.withLimit(10);

			if (cursorString != null) {
				fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
			}

			QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
			cursorString = results.getCursor().toWebSafeString();

			return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
		}
		
//------------------------------------POST ENTITY METHODS--------------------------------------------//	
		
		
	//Post new message
	@ApiMethod(name = "postMsg", httpMethod = HttpMethod.POST)
	public Entity postMsg(User user, PostMessage pm) throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Entity e = new Entity("TGPost", Long.MAX_VALUE-(new Date()).getTime()+":"+user.getEmail());
		e.setProperty("owner", user.getEmail());
		e.setProperty("url", pm.url);
		e.setProperty("body", pm.body);
		e.setProperty("likec", 0);
		e.setProperty("date", new Date());

		// Solution pour pas projeter les listes
		// Entity pi = new Entity("PostIndex", e.getKey());
		// HashSet<String> rec=new HashSet<String>();
		// pi.setProperty("receivers",rec);
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
		// datastore.put(pi);
		txn.commit();
		return e;
	}
	 
	
	//User's posts    
	@ApiMethod(name = "getTGPost", httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Entity> getTGPost(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
		Query q = new Query("TGPost").
		    setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, user.getEmail()));

		// Multiple projection require a composite index
		// owner is automatically projected...
		// q.addProjection(new PropertyProjection("body", String.class));
		// q.addProjection(new PropertyProjection("date", java.util.Date.class));
		// q.addProjection(new PropertyProjection("likec", Integer.class));
		// q.addProjection(new PropertyProjection("url", String.class));

		// looks like a good idea but...
		// require a composite index
		// - kind: Post
		//  properties:
		//  - name: owner
		//  - name: date
		//    direction: desc

		// q.addSort("date", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(5);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
		cursorString = results.getCursor().toWebSafeString();

		return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

	
}
