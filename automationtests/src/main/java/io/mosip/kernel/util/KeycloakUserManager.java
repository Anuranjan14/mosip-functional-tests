package io.mosip.kernel.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.mosip.service.BaseTestCase;
import io.mosip.testrunner.MosipTestRunner;



public class KeycloakUserManager {
	
	private static final Logger logger = Logger.getLogger(KeycloakUserManager.class);

	public static Properties propsKernel = getproperty(MosipTestRunner.getResourcePath() + "/"+"config/Kernel.properties");

	private static Keycloak getKeycloakInstance() {
		 Keycloak key=null;
		try {
			
	key=KeycloakBuilder.builder().serverUrl(ConfigManager.getIAMUrl()).realm(ConfigManager.getIAMRealmId())
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS).clientId(ConfigManager.getAutomationClientId()).clientSecret(ConfigManager.getAutomationClientSecret())
				.build();
	System.out.println(ConfigManager.getIAMUrl());
	System.out.println(key.toString() + key.realms());
		}catch(Exception e)
		{
			throw e;
			
		}
		return key;
	}

	public static Properties getproperty(String path) {
		Properties prop = new Properties();		
		try {
			File file = new File(path);
			prop.load(new FileInputStream(file));
		} catch (IOException e) {
			logger.error("Exception " + e.getMessage());
		}
		return prop;
	}

	public static void createUsers() {
		
		List<String> needsToBeCreatedUsers = List.of(ConfigManager.getIAMUsersToCreate().split(","));
		Keycloak keycloakInstance = getKeycloakInstance();
		for (String needsToBeCreatedUser : needsToBeCreatedUsers) {
			UserRepresentation user = new UserRepresentation();
			String moduleSpecificUser = null;
			if (needsToBeCreatedUser.equals("globaladmin")) {
				moduleSpecificUser = needsToBeCreatedUser;
			}
			else if(needsToBeCreatedUser.equals("masterdata-220005")){
				moduleSpecificUser = needsToBeCreatedUser;
				
			}
			
			else {
				moduleSpecificUser = BaseTestCase.currentModule +"-"+ needsToBeCreatedUser;
			}
			
			System.out.println(moduleSpecificUser);
			user.setEnabled(true);
			user.setUsername(moduleSpecificUser);
			user.setFirstName(moduleSpecificUser);
			user.setLastName(moduleSpecificUser);
			user.setEmail("automation" + moduleSpecificUser + "@automationlabs.com");
			// Get realm
			RealmResource realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();
			// Create user (requires manage-users role)
			Response response = null;
				response = usersRessource.create(user);
 				System.out.println(response);
			System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
			if (response.getStatus()==409) {
				continue;
			}
			System.out.println(response.getLocation());
			String userId = CreatedResponseUtil.getCreatedId(response);
			System.out.printf("User created with userId: %s%n", userId);

			// Define password credential
			CredentialRepresentation passwordCred = new CredentialRepresentation();
			
			passwordCred.setTemporary(false);
			passwordCred.setType(CredentialRepresentation.PASSWORD);
			
			//passwordCred.setValue(userPassword.get(passwordIndex));
			passwordCred.setValue("mosip123");

			UserResource userResource = usersRessource.get(userId);

			// Set password credential
			userResource.resetPassword(passwordCred);

			// Getting all the roles
			List<RoleRepresentation> allRoles = realmResource.roles().list();
			List<RoleRepresentation> availableRoles = new ArrayList<>();
			List<String> toBeAssignedRoles = List.of(ConfigManager.getRolesForUser(needsToBeCreatedUser).split(","));
			for(String role : toBeAssignedRoles) {
				if(allRoles.stream().anyMatch((r->r.getName().equalsIgnoreCase(role)))){
					availableRoles.add(allRoles.stream().filter(r->r.getName().equals(role)).findFirst().get());
				}else {
					System.out.printf("Role not found in keycloak: %s%n", role);
				}
			}
			// Assign realm role tester to user
			userResource.roles().realmLevel() //
					.add((availableRoles.isEmpty() ? allRoles : availableRoles));
			//passwordIndex ++;
		}
	}

	
	public static void createKeyCloakUsers(String partnerId,String emailId, String userRole ) {
		List<String> needsToBeCreatedUsers = List.of(partnerId);
		//List<String> userPassword = List.of(propsKernel.getProperty("users.password").split(","));
		Keycloak keycloakInstance = getKeycloakInstance();
		//int passwordIndex = 0;
		for (String needsToBeCreatedUser : needsToBeCreatedUsers) {
			UserRepresentation user = new UserRepresentation();
			user.setEnabled(true);
			user.setUsername(needsToBeCreatedUser);
			user.setFirstName(needsToBeCreatedUser);
			user.setLastName("");
			user.setEmail(emailId);
			// Get realm
			RealmResource realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();
			// Create user (requires manage-users role)
			Response response = usersRessource.create(user);
			System.out.println(response);
			System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
			System.out.println(response.getLocation());
			String userId = CreatedResponseUtil.getCreatedId(response);
			System.out.printf("User created with userId: %s%n", userId);

			// Define password credential
			CredentialRepresentation passwordCred = new CredentialRepresentation();
			
			passwordCred.setTemporary(false);
			passwordCred.setType(CredentialRepresentation.PASSWORD);
			
			//passwordCred.setValue(userPassword.get(passwordIndex));
			passwordCred.setValue("mosip123");

			UserResource userResource = usersRessource.get(userId);

			// Set password credential
			userResource.resetPassword(passwordCred);

			// Getting all the roles
			List<RoleRepresentation> allRoles = realmResource.roles().list();
			List<RoleRepresentation> availableRoles = new ArrayList<>();
			List<String> toBeAssignedRoles = List.of(userRole);
			for(String role : toBeAssignedRoles) {
				if(allRoles.stream().anyMatch((r->r.getName().equalsIgnoreCase(role)))){
					availableRoles.add(allRoles.stream().filter(r->r.getName().equals(role)).findFirst().get());
				}else {
					System.out.printf("Role not found in keycloak: %s%n", role);
				}
			}
			// Assign realm role tester to user
			userResource.roles().realmLevel() //
					.add((availableRoles.isEmpty() ? allRoles : availableRoles));
			//passwordIndex ++;
		}
	}
	
	public static void removeKeyCloakUser(String partnerId) {
		List<String> needsToBeRemovedUsers = List.of(partnerId);
		Keycloak keycloakInstance = getKeycloakInstance();
		for (String needsToBeRemovedUser : needsToBeRemovedUsers) {
			RealmResource realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();

			List<UserRepresentation> usersFromDB = usersRessource.search(needsToBeRemovedUser);
			if (!usersFromDB.isEmpty()) {
				UserResource userResource = usersRessource.get(usersFromDB.get(0).getId());
				userResource.remove();
				System.out.printf("User removed with name: %s%n", needsToBeRemovedUser);
			} else {
				System.out.printf("User not found with name: %s%n", needsToBeRemovedUser);
			}

		}
	}
	
	public static void removeUser() {
		List<String> needsToBeRemovedUsers = List.of(ConfigManager.getIAMUsersToCreate().split(","));
		Keycloak keycloakInstance = getKeycloakInstance();
		for (String needsToBeRemovedUser : needsToBeRemovedUsers) {
			String moduleSpecificUserToBeRemoved = BaseTestCase.currentModule +"-"+ needsToBeRemovedUser;
			RealmResource realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();

			List<UserRepresentation> usersFromDB = usersRessource.search(moduleSpecificUserToBeRemoved);
			if (!usersFromDB.isEmpty()) {
				UserResource userResource = usersRessource.get(usersFromDB.get(0).getId());
				userResource.remove();
				System.out.printf("User removed with name: %s%n", moduleSpecificUserToBeRemoved);
			} else {
				System.out.printf("User not found with name: %s%n", moduleSpecificUserToBeRemoved);
			}

		}
	}
	public static void removeVidUser() {
		List<String> needsToBeRemovedUsers = List.of(BaseTestCase.currentModule +"-"+propsKernel.getProperty("new_Resident_User"));
		Keycloak keycloakInstance = getKeycloakInstance();
		for (String needsToBeRemovedUser : needsToBeRemovedUsers) {
			RealmResource realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();

			List<UserRepresentation> usersFromDB = usersRessource.search(needsToBeRemovedUser);
			if (!usersFromDB.isEmpty()) {
				UserResource userResource = usersRessource.get(usersFromDB.get(0).getId());
				userResource.remove();
				System.out.printf("User removed with name: %s%n", needsToBeRemovedUser);
			} else {
				System.out.printf("User not found with name: %s%n", needsToBeRemovedUser);
			}

		}
	}

	public static void createUsers(String userid,String pwd, String rolenum,HashMap<String, List<String>> map) {
		Keycloak keycloakInstance = getKeycloakInstance();
			UserRepresentation user = new UserRepresentation(); 
			user.setEnabled(true);
			user.setUsername(userid);
			user.setFirstName(userid);
			user.setLastName(userid);
			user.setEmail("automation" + userid + "@automationlabs.com");
			if(map!=null)
		    user.setAttributes(map);
			// Get realm
			RealmResource realmResource=null;
			 realmResource = keycloakInstance.realm(propsKernel.getProperty("keycloak-realm-id"));
			UsersResource usersRessource = realmResource.users();
			// Create user (requires manage-users role)
			Response response = usersRessource.create(user);
			System.out.println(response);
			System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
			System.out.println(response.getLocation());
			String userId = CreatedResponseUtil.getCreatedId(response);
			System.out.printf("User created with userId: %s%n", userId);

			// Define password credential
			CredentialRepresentation passwordCred = new CredentialRepresentation();
			
			passwordCred.setTemporary(false);
			passwordCred.setType(CredentialRepresentation.PASSWORD);
			
			//passwordCred.setValue(userPassword.get(passwordIndex));
			passwordCred.setValue(pwd);

			UserResource userResource = usersRessource.get(userId);
			// Set password credential
			userResource.resetPassword(passwordCred);

			// Getting all the roles
			List<RoleRepresentation> allRoles = realmResource.roles().list();
			List<RoleRepresentation> availableRoles = new ArrayList<>();
			List<String> toBeAssignedRoles = List.of(propsKernel.getProperty(rolenum).split(","));
			for(String role : toBeAssignedRoles) {
				if(allRoles.stream().anyMatch((r->r.getName().equalsIgnoreCase(role)))){
					availableRoles.add(allRoles.stream().filter(r->r.getName().equals(role)).findFirst().get());
				}else {
					System.out.printf("Role not found in keycloak: %s%n", role);
				}
			}
			// Assign realm role tester to user
			userResource.roles().realmLevel() //
					.add((availableRoles.isEmpty() ? allRoles : availableRoles));
			//passwordIndex ++;
		
	}
	
	public static void createUsersWithArg(String userid,String pwd, String rolenum) {
		Keycloak keycloakInstance = getKeycloakInstance();
			UserRepresentation user = new UserRepresentation(); 
			user.setEnabled(true);
			user.setUsername(userid);
			user.setFirstName(userid);
			user.setLastName(userid);
			user.setEmail("automation" + userid + "@automationlabs.com");
			// Get realm
			RealmResource realmResource=null;
			 realmResource = keycloakInstance.realm(propsKernel.getProperty("keycloak.realm"));
			UsersResource usersRessource = realmResource.users();
			// Create user (requires manage-users role)
			Response response = usersRessource.create(user);
			System.out.println(response);
			System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
			System.out.println(response.getLocation());
			String userId = CreatedResponseUtil.getCreatedId(response);
			System.out.printf("User created with userId: %s%n", userId);

			// Define password credential
			CredentialRepresentation passwordCred = new CredentialRepresentation();
			
			passwordCred.setTemporary(false);
			passwordCred.setType(CredentialRepresentation.PASSWORD);
			
			//passwordCred.setValue(userPassword.get(passwordIndex));
			passwordCred.setValue(pwd);

			UserResource userResource = usersRessource.get(userId);
			// Set password credential
			userResource.resetPassword(passwordCred);

			// Getting all the roles
			List<RoleRepresentation> allRoles = realmResource.roles().list();
			List<RoleRepresentation> availableRoles = new ArrayList<>();
			List<String> toBeAssignedRoles = List.of(propsKernel.getProperty(rolenum).split(","));
			for(String role : toBeAssignedRoles) {
				if(allRoles.stream().anyMatch((r->r.getName().equalsIgnoreCase(role)))){
					availableRoles.add(allRoles.stream().filter(r->r.getName().equals(role)).findFirst().get());
				}else {
					System.out.printf("Role not found in keycloak: %s%n", role);
				}
			}
			// Assign realm role tester to user
			userResource.roles().realmLevel() //
					.add((availableRoles.isEmpty() ? allRoles : availableRoles));
			//passwordIndex ++;
		
	}
	public static void createVidUsers(String userid,String pwd, String rolenum,HashMap<String, List<String>> map) {
		Keycloak keycloakInstance = getKeycloakInstance();
			UserRepresentation user = new UserRepresentation();
			
			String moduleSpecificUser = null;
			moduleSpecificUser = BaseTestCase.currentModule +"-"+userid;
			
			user.setEnabled(true);
			user.setUsername(moduleSpecificUser);
			user.setFirstName(moduleSpecificUser);
			user.setLastName(moduleSpecificUser);
			user.setEmail("automation" + moduleSpecificUser + "@automationlabs.com");
			if(map!=null)
		    user.setAttributes(map);
			// Get realm
			RealmResource realmResource=null;
			 realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();
			// Create user (requires manage-users role)
			Response response = usersRessource.create(user);
			System.out.println(response);
			System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
			
			
			System.out.println(response.getLocation());
			String userId = CreatedResponseUtil.getCreatedId(response);
			System.out.printf("User created with userId: %s%n", userId);

			// Define password credential
			CredentialRepresentation passwordCred = new CredentialRepresentation();
			
			passwordCred.setTemporary(false);
			passwordCred.setType(CredentialRepresentation.PASSWORD);
			
			//passwordCred.setValue(userPassword.get(passwordIndex));
			passwordCred.setValue("mosip123");

			UserResource userResource = usersRessource.get(userId);
			// Set password credential
			userResource.resetPassword(passwordCred);

			// Getting all the roles
			List<RoleRepresentation> allRoles = realmResource.roles().list();
			List<RoleRepresentation> availableRoles = new ArrayList<>();
			List<String> toBeAssignedRoles = List.of(propsKernel.getProperty("roles."+userid).split(","));
			for(String role : toBeAssignedRoles) {
				if(allRoles.stream().anyMatch((r->r.getName().equalsIgnoreCase(role)))){
					availableRoles.add(allRoles.stream().filter(r->r.getName().equals(role)).findFirst().get());
				}else {
					System.out.printf("Role not found in keycloak: %s%n", role);
				}
			}
			// Assign realm role tester to user
			userResource.roles().realmLevel() //
					.add((availableRoles.isEmpty() ? allRoles : availableRoles));
			//passwordIndex ++;
		
	}
	public static void removeUser(String user) {
		Keycloak keycloakInstance = getKeycloakInstance();
			RealmResource realmResource = keycloakInstance.realm(ConfigManager.getIAMRealmId());
			UsersResource usersRessource = realmResource.users();

			List<UserRepresentation> usersFromDB = usersRessource.search(user);
			if (!usersFromDB.isEmpty()) {
				UserResource userResource = usersRessource.get(usersFromDB.get(0).getId());
				userResource.remove();
				System.out.printf("User removed with name: %s%n", user);
			} else {
				System.out.printf("User not found with name: %s%n", user);
			}

		
	}

}
