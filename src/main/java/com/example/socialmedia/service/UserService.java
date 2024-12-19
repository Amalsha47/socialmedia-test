package com.example.socialmedia.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.socialmedia.model.DoubleIdObjectEntity;
import com.example.socialmedia.model.IdObjectEntity;
import com.example.socialmedia.model.User;
import com.example.socialmedia.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptEncoder;

    public ResponseObjectService findAll() {
        ResponseObjectService responseObj = new ResponseObjectService();
        responseObj.setPayload(userRepository.findAll());
        responseObj.setStatus("success");
        responseObj.setMessage("success");
        return responseObj;
    }

    public ResponseObjectService findById(String id) {
        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("user id: " + id + " not existed");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            responseObj.setPayload(optUser.get());
            responseObj.setStatus("success");
            responseObj.setMessage("success");
            return responseObj;
        }
    }

    public ResponseObjectService findFollowing(String id) {
        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("user id: " + id + " not existed");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            List<String> followingIds = optUser.get().getFollowing();
            List<User> followingAccounts = new ArrayList<>();
            if (followingIds.size() > 0) {
                for (String followingId : followingIds) {
                    Optional<User> optFollowingUser = userRepository.findById(followingId);
                    if (optFollowingUser.isPresent()) {
                        User followingUser = optFollowingUser.get();
                        followingUser.setPassword("");
                        followingAccounts.add(followingUser);
                    }
                }
                responseObj.setStatus("success");
                responseObj.setMessage("success");
                responseObj.setPayload(followingAccounts);
                return responseObj;
            } else {
                responseObj.setStatus("fail");
                responseObj.setMessage("User id " + id + " does not follow anyone");
                responseObj.setPayload(null);
                return responseObj;
            }
        }
    }

    public ResponseObjectService findFollower(String id) {
        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("user id: " + id + " not existed");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            List<String> followerIds = optUser.get().getFollower();
            List<User> followerAccounts = new ArrayList<>();
            if (followerIds.size() > 0) {
                for (String followerId : followerIds) {
                    Optional<User> optFollowerUser = userRepository.findById(followerId);
                    if (optFollowerUser.isPresent()) {
                        User followerUser = optFollowerUser.get();
                        followerUser.setPassword("");
                        followerAccounts.add(followerUser);
                    }
                }
                responseObj.setStatus("success");
                responseObj.setMessage("success");
                responseObj.setPayload(followerAccounts);
                return responseObj;
            } else {
                responseObj.setStatus("fail");
                responseObj.setMessage("User id " + id + " does not have any follower");
                responseObj.setPayload(null);
                return responseObj;
            }
        }
    }

    public ResponseObjectService saveUser(User inputUser) {
        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optUser = userRepository.findByEmail(inputUser.getEmail());
        if (optUser.isPresent()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("Email address " + inputUser.getEmail() + " existed");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            inputUser.setPassword(bCryptEncoder.encode(inputUser.getPassword()));

            // user follows himself so he could get his posts in newsfeed as well
            User user = userRepository.save(inputUser);
            List<String> listFollowing = user.getFollowing();
            if (listFollowing == null) {
                listFollowing = new ArrayList<>();
            }
            listFollowing.add(user.getId());
            user.setFollowing(listFollowing);
            this.updateWithoutPassword(user);
            responseObj.setPayload(user);
            responseObj.setStatus("success");
            responseObj.setMessage("success");
            return responseObj;
        }
    }

    public boolean updateWithoutPassword(User inputUser) {
        Optional<User> optUser = userRepository.findById(inputUser.getId());
        if (optUser.isEmpty()) {
            return false;
        } else {
            User currentUser = optUser.get();
            if (inputUser.getPassword().equals(currentUser.getPassword())) {
                userRepository.save(inputUser);
                return true;
            } else {
                return false;
            }
        }
    }

    public ResponseObjectService update(User inputUser) {
        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optUser = userRepository.findById(inputUser.getId());
        if (optUser.isEmpty()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("user id: " + inputUser.getId() + " not existed");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            User currentUser = optUser.get();
            if (bCryptEncoder.matches(inputUser.getPassword(), currentUser.getPassword())) {
                inputUser.setPassword(bCryptEncoder.encode(inputUser.getPassword()));
                responseObj.setPayload(userRepository.save(inputUser));
                responseObj.setStatus("success");
                responseObj.setMessage("success");
                return responseObj;
            } else {
                responseObj.setStatus("fail");
                responseObj.setMessage("current password is not correct");
                responseObj.setPayload(null);
                return responseObj;
            }
        }
    }

    public ResponseObjectService followUser(DoubleIdObjectEntity doubleId) {
        // id1 - followed user, id2 - follower

        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optFollowedUser = userRepository.findById(doubleId.getId1());
        Optional<User> optFollower = userRepository.findById(doubleId.getId2());
        if (optFollowedUser.isEmpty() || optFollower.isEmpty()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("invalid user id");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            User followedUser = optFollowedUser.get();
            User follower = optFollower.get();

            // add to follower list
            List<String> followerList = followedUser.getFollower();
            if (followerList == null) {
                followerList = new ArrayList<>();
            }
            followerList.add(follower.getId());
            followedUser.setFollower(followerList);

            // add to following list
            List<String> followingList = follower.getFollowing();
            if (followingList == null) {
                followingList = new ArrayList<>();
            }
            followingList.add(followedUser.getId());
            follower.setFollowing(followingList);

            userRepository.save(followedUser);
            userRepository.save(follower);

            responseObj.setStatus("success");
            responseObj.setMessage(
                    "User id " + follower.getId() + " successfully followed user id " + followedUser.getId());
            responseObj.setPayload(new IdObjectEntity(doubleId.getId1()));
            return responseObj;
        }
    }

    public ResponseObjectService unfollowUser(DoubleIdObjectEntity doubleId) {
        // id1 - followed user, id2 - follower

        ResponseObjectService responseObj = new ResponseObjectService();
        Optional<User> optFollowedUser = userRepository.findById(doubleId.getId1());
        Optional<User> optFollower = userRepository.findById(doubleId.getId2());
        if (optFollowedUser.isEmpty() || optFollower.isEmpty()) {
            responseObj.setStatus("fail");
            responseObj.setMessage("invalid user id");
            responseObj.setPayload(null);
            return responseObj;
        } else {
            User followedUser = optFollowedUser.get();
            User follower = optFollower.get();

            // add to follower list
            List<String> followerList = followedUser.getFollower();
            if (followerList == null) {
                followerList = new ArrayList<>();
            }
            followerList.remove(follower.getId());
            followedUser.setFollower(followerList);

            // add to following list
            List<String> followingList = follower.getFollowing();
            if (followingList == null) {
                followingList = new ArrayList<>();
            }
            followingList.remove(followedUser.getId());
            follower.setFollowing(followingList);

            userRepository.save(followedUser);
            userRepository.save(follower);

            responseObj.setStatus("success");
            responseObj.setMessage(
                    "User id " + follower.getId() + " successfully unfollowed user id " + followedUser.getId());
            responseObj.setPayload(new IdObjectEntity(doubleId.getId1()));
            return responseObj;
        }
    }

    // important for security
    // Use user email as unique field to login instead of username
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> optUser = userRepository.findByEmail(email);
        User springUser = null;

        if (optUser.isEmpty()) {
            throw new UsernameNotFoundException("Cannot find user with email: " + email);
        } else {
            User foundUser = optUser.get();
            String role = foundUser.getRole();
            Set<GrantedAuthority> ga = new HashSet<>();
            ga.add(new SimpleGrantedAuthority(role));
            springUser = new User();
            return (UserDetails) springUser;
        }
    }
}
