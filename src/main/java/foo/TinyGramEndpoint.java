package foo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;

@Api(name = "myApi", version = "v1", audiences = "331048952451-9k74vlboc2n4vvh12jkct30fhc2euc85.apps.googleusercontent.com", clientIds = "331048952451-9k74vlboc2n4vvh12jkct30fhc2euc85.apps.googleusercontent.com", namespace = @ApiNamespace(ownerDomain = "helloworld.example.com", ownerName = "helloworld.example.com", packagePath = ""))

public class TinyGramEndpoint {

//----------------------------------------------USER ENTITY METHODS-------------------------------------------------------//     

	/**
	 * Checking if user has already added. If no, add him
	 * 
	 * @param user
	 * @param up
	 * @return Entity TGUser
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "userManager", path = "users/userManager", httpMethod = HttpMethod.GET)
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
			e.setProperty("name", up.getName());
			e.setProperty("email", user.getEmail());

			String[] parts = user.getEmail().split("@");
			String part1 = parts[0];
			e.setProperty("pseudo", "@" + part1);
			e.setProperty("bio", "I'm a human, funny and cool. Follow me");
			e.setProperty("profilUrl", up.getProfilUrl());

			List<String> followings = new ArrayList<String>();
			List<String> followers = new ArrayList<String>();

			e.setProperty("followings", followings);
			e.setProperty("followers", followers);

			Transaction txn = datastore.beginTransaction();
			datastore.put(e);
			txn.commit();

			// To verify if the new entity was really created

			try {

				e = datastore.get(e.getKey());

			} catch (EntityNotFoundException nef2) {
				nef2.printStackTrace();
			}

		}

		return e;
	}

//---------------------------------------------------------------------------------------------------------------------//
	/**
	 * Retrieving given user by ID
	 * 
	 * @param user
	 * @param up
	 * @return Entity TGUser
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "getUser", path = "users/{id}", httpMethod = HttpMethod.GET)
	public Entity getUser(User user, @Named("id") String id) throws UnauthorizedException {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Query q = new Query("TGUser")
				.setFilter(new FilterPredicate("__key__", FilterOperator.EQUAL, KeyFactory.stringToKey(id)));
		PreparedQuery pq = datastore.prepare(q);
		Entity eu = pq.asSingleEntity();

		return eu;

	}

//---------------------------------------------------------------------------------------------------------------------//		

	/**
	 * collection of all TGUser entities
	 * 
	 * @param user
	 * @param cursorString
	 * @return CollectionResponse<Entity> TGUser
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "get_users", path = "users", httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Entity> getTGUsers(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Query q = new Query("TGUser");

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(20);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
		cursorString = results.getCursor().toWebSafeString();

		return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

//------------------------------------------------------------------------------------------------------------------------//
	/**
	 * Querying current user's followers
	 * @param user
	 * @return CollectionResponse<Entity> TGUser
	 * @throws UnauthorizedException 
	 */
	@ApiMethod(name = "followers", path = "users/followers", httpMethod = HttpMethod.GET)
	public List<String> getUserFollowers(User user, @Nullable @Named("next") String cursorString) throws UnauthorizedException {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
		
		Key userKey = KeyFactory.createKey("TGUser", user.getEmail());
		
		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(20);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}
		
		Query q = new Query("TGUser").setFilter(new FilterPredicate("__key__", FilterOperator.EQUAL, userKey));
		PreparedQuery pq = datastore.prepare(q);
		Entity u = pq.asSingleEntity();
		List<String> followers = (List<String>) u.getProperty("followers");

		return followers;
}
//------------------------------------------------------------------------------------------------------------------------//
	/**
	 * Querying current user's followings
	 * @param user
	 * @return CollectionResponse<Entity> TGUser
	 * @throws UnauthorizedException 
	 */
	@ApiMethod(name = "followings", path = "users/followings", httpMethod = HttpMethod.GET)
	public List<String> getUserFollowings(User user, @Nullable @Named("next") String cursorString) throws UnauthorizedException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
		
		Key userKey = KeyFactory.createKey("TGUser", user.getEmail());
				
		Query q = new Query("TGUser").setFilter(new FilterPredicate("__key__", FilterOperator.EQUAL, userKey));
		PreparedQuery pq = datastore.prepare(q);
		Entity u = pq.asSingleEntity();
		List<String> followings = (List<String>) u.getProperty("followings");

		return followings;
	}
	//------------------------------------------------------------------------------------------------------------------------//
		
		@ApiMethod(name = "explore", path = "users/explore", httpMethod = HttpMethod.GET)
		public CollectionResponse<Entity> getExplore(User user, @Nullable @Named("next") String cursorString)
				throws UnauthorizedException, EntityNotFoundException {
			
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			
			if (user == null) {
				throw new UnauthorizedException("Invalid credentials");
			}
			
			Key userKey = KeyFactory.createKey("TGUser", user.getEmail());
			List<String> followings = new ArrayList<String>();
			
			Entity u;
			try {

				u = datastore.get(userKey);
				followings = (List<String>) u.getProperty("followings");
			} catch (EntityNotFoundException nef) { 
				
			}
			
			Query q = new Query("TGUser");

			
			PreparedQuery pq = datastore.prepare(q);

			FetchOptions fetchOptions = FetchOptions.Builder.withLimit(15);

			if (cursorString != null) {
				fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
			}

			QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
			
			if(followings!=null) {
				for (Entity e : results) {				
					if(followings.contains(e.getProperty("email"))) {
						results.remove(e);
					}
				}	
			}
			cursorString = results.getCursor().toWebSafeString();

			return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
		}

//------------------------------------------------------------------------------------------------------------------------//
	/**
	 * Following a user
	 * 
	 * @param user
	 * @param up
	 * 
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "followUser", path = "users/follow", httpMethod = HttpMethod.PUT)
	public void followUser(User user, TGUserProfil up) throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Transaction txn = datastore.beginTransaction();
		try {
			Key userKey = KeyFactory.createKey("TGUser", user.getEmail());
			Entity eu = datastore.get(userKey);

			// Adding the new following to the followings set if it not exists yet

			List<String> followings = (List<String>) eu.getProperty("followings");
			if (followings == null) {
				followings = new ArrayList<String>();
			}
			if (!followings.contains(up.getEmail())) {
				followings.add(up.getEmail());
			}
			eu.setProperty("followings", followings);

			// Now adding the the current user to the new following's followers set

			Key newFollowingKey = KeyFactory.createKey("TGUser", up.getEmail());
			Entity newFollowing = datastore.get(newFollowingKey);

			List<String> followers = (List<String>) newFollowing.getProperty("followers");
			if (followers == null) {
				followers = new ArrayList<String>();
			}
			if (!followers.contains(user.getEmail())) {
				followers.add(user.getEmail());
			}
			newFollowing.setProperty("followers", followers);

			datastore.put(txn, eu);
			datastore.put(txn, newFollowing);
			txn.commit();

		} catch (EntityNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (txn.isActive()) {
				txn.rollback();

			}
		}

	}

//------------------------------------------------------------------------------------------------------------------------//
	/**
	 * Unfollowing a user
	 * 
	 * @param user
	 * @param up
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "unfollowUser", path = "users/unfollow", httpMethod = HttpMethod.PUT)
	public void unfollowUser(User user, TGUserProfil up) throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Transaction txn = datastore.beginTransaction();
		try {

			Key userKey = KeyFactory.createKey("TGUser", user.getEmail());
			Entity eu = datastore.get(userKey);

			// Removing the user to the current user's followings set if it exists

			List<String> followings = (List<String>) eu.getProperty("followings");

			if (followings.contains(up.getEmail())) {
				followings.remove(up.getEmail());
			}
			eu.setProperty("followings", followings);

			// Now removing also the current user to the old following's followers set

			Key oldFollowingKey = KeyFactory.createKey("TGUser", up.getEmail());
			Entity oldFollowing = datastore.get(oldFollowingKey);

			List<String> followers = (List<String>) oldFollowing.getProperty("followers");

			if (followers.contains(user.getEmail())) {
				followers.remove(user.getEmail());
			}
			oldFollowing.setProperty("followers", followers);

			datastore.put(txn, eu);
			datastore.put(txn, oldFollowing);
			txn.commit();

		} catch (EntityNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (txn.isActive()) {
				txn.rollback();

			}
		}

	}
//-----------------------------------------------------------------------------------------------------------------------//		
//------------------------------------POST ENTITY RELATED METHODS---------------------------------------------------------//	

	/**
	 * Post a new message
	 * 
	 * @param user
	 * @param pm
	 * @return Entity TGPost
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "postMsg", path = "posts/new", httpMethod = HttpMethod.POST)
	public Entity postMsg(User user, PostMessage pm) throws UnauthorizedException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
		
		Key userKey = KeyFactory.createKey("TGUser", user.getEmail());

        List<String> receivers = new ArrayList<String>();
		Entity u;
		try {

			u = datastore.get(userKey);
			receivers = (List<String>) u.getProperty("followers");
            
		} catch (EntityNotFoundException nef) { }
		
		Entity e = new Entity("TGPost", Long.MAX_VALUE - (new Date()).getTime() + ":" + user.getEmail());
		e.setProperty("owner", user.getEmail());
		e.setProperty("ownerHashed", KeyFactory.keyToString(userKey)); //pour pour la reutiliser lors de la recherche de la timeline
		e.setProperty("url", pm.getUrl());
		e.setProperty("body", pm.getBody());
		e.setProperty("likec", 0);
		e.setProperty("date", new Date());
		e.setProperty("receivers",receivers); 

		
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);

		txn.commit();
		return e;
	}

	/**
	 * Collection Entities of TGPost for the current user
	 * 
	 * @param user
	 * @param cursorString
	 * @return CollectionResponse<Entity> TGPost
	 * @throws UnauthorizedException
	 */
	@ApiMethod(name = "getTGPost", path = "posts/owner", httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Entity> getTGPost(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
		Query q = new Query("TGPost").setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, user.getEmail()));

		// Multiple projection require a composite index
		// owner is automatically projected...
		// q.addProjection(new PropertyProjection("body", String.class));
		// q.addProjection(new PropertyProjection("date", java.util.Date.class));
		// q.addProjection(new PropertyProjection("likec", Integer.class));
		// q.addProjection(new PropertyProjection("url", String.class));

		// looks like a good idea but...
		// require a composite index
		// - kind: Post
		// properties:
		// - name: owner
		// - name: date
		// direction: desc

		// q.addSort("date", SortDirection.DESCENDING);

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

	/**
	 * the current user time line
	 * 
	 * @param user
	 * @param cursorString
	 * @return CollectionResponse<Entity> TGPost
	 * @throws UnauthorizedException
	 * @throws EntityNotFoundException
	 */
	@ApiMethod(name = "userTimeLine", path = "posts/user/timeline", httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Entity> getUserTimeLine(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException, EntityNotFoundException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Query q = new Query("TGPost").setFilter(new FilterPredicate("receivers", FilterOperator.EQUAL, user.getEmail()));

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(15);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
		cursorString = results.getCursor().toWebSafeString();

		return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}
//---------------------------------------------------------------------------------------------------------------------//
	/**
	 * Liking a post
	 * 
	 * @param user
	 * @param pm
	 * @return Entity TGPost
	 * @throws UnauthorizedException
	 * @throws EntityNotFoundException
	 */
	@ApiMethod(name = "likePost", path = "posts/like", httpMethod = HttpMethod.PUT)
	public Entity likepost(User user, PostMessage pm) throws UnauthorizedException, EntityNotFoundException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Key postKey = KeyFactory.createKey("TGPost", pm.getID());
		Entity post = datastore.get(postKey);

		Transaction txn = datastore.beginTransaction();

		try {

			List<String> likes = (List<String>) post.getProperty("likes");

			if (likes == null) {
				likes = new ArrayList<String>();
			}

			if (!likes.contains(user.getEmail())) {
				likes.add(user.getEmail());
			}
			post.setProperty("likes", likes);

			// updating likes counter
			post.setProperty("likec", likes.size());
			datastore.put(txn, post);
			txn.commit();

		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		return post;

	}

	// ---------------------------------------------------------------------------------------------------------------------//
	/**
	 * Unliking a post
	 * 
	 * @param user
	 * @param pm
	 * @return Entity TGPost
	 * @throws UnauthorizedException
	 * @throws EntityNotFoundException
	 */
	@ApiMethod(name = "unlikePost", path = "posts/unlike", httpMethod = HttpMethod.PUT)
	public Entity unlikepost(User user, PostMessage pm) throws UnauthorizedException, EntityNotFoundException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Key postKey = KeyFactory.createKey("TGPost", pm.getID());
		Entity post = datastore.get(postKey);
        
		Transaction txn = datastore.beginTransaction();

		try {

			List<String> likes = (List<String>) post.getProperty("likes");

			if (likes.contains(user.getEmail())) {
				likes.remove(user.getEmail());
			}

			post.setProperty("likes", likes);
			// updating likes counter after adding a like
			post.setProperty("likec", likes.size());

			datastore.put(txn, post);
			txn.commit();

		} finally {
			if (txn.isActive()) {
				txn.rollback();
				
			}
		}

		return post;

	}

	// ---------------------------------------------------------------------------------------------------------------------//	
	@ApiMethod(name = "deletePost", path = "posts/delete", httpMethod = HttpMethod.DELETE)
	public Entity deletePost(User user, PostMessage pm) throws UnauthorizedException, EntityNotFoundException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Key postKey = KeyFactory.createKey("TGPost", pm.getID());
		Entity post = datastore.get(postKey);
		
		Transaction txn = datastore.beginTransaction();

		try {
			datastore.delete(postKey);
			txn.commit();

		} finally {
			if (txn.isActive()) {
				txn.rollback();
				
			}
		}

		return post;

	}
}
