package kr.co.api.flobankapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
public class TestExperienceController {

    @GetMapping({"", "/"})
    public String overview() {
        return "test/creator_portal";
    }

    @GetMapping("/creator")
    public String creatorHub() {
        return "test/creator_portal";
    }

    @GetMapping("/followers")
    public String followerInsight() {
        return "test/follower_insight";
    }

    @GetMapping("/events")
    public String eventCampaign() {
        return "test/event_campaign";
    }
}
