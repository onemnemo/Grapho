use super::*;

const RULER: Guide = Guide::Ruler {
    centre: [100.0, 100.0],
    angle: 0.0,
    length: 300.0,
    thickness: 30.0,
};

fn protractor(angle: f32) -> Guide {
    Guide::Protractor {
        centre: [0.0, 0.0],
        angle,
        radius: 100.0,
    }
}

#[test]
fn a_pen_near_a_long_edge_is_projected_beside_it() {
    // The top edge of the ruler is y = 85, and ink sits above it.
    let (edge, d) = RULER.nearest_edge(120.0, 80.0, 10.0).expect("near the top");
    assert!((d - 5.0).abs() < 1e-4);
    let (x, y) = edge.project(120.0, 80.0, 2.0);
    assert!((x - 120.0).abs() < 1e-4);
    assert!((y - 83.0).abs() < 1e-4, "{y}");
    // The bottom edge is y = 115, and ink sits below it.
    let (edge, _) = RULER
        .nearest_edge(10.0, 118.0, 10.0)
        .expect("near the bottom");
    assert!((edge.project(10.0, 118.0, 2.0).1 - 117.0).abs() < 1e-4);
}

#[test]
fn a_pen_in_the_middle_or_far_away_draws_freely() {
    assert!(RULER.nearest_edge(100.0, 100.0, 10.0).is_none());
    assert!(RULER.nearest_edge(100.0, 40.0, 10.0).is_none());
    // Past the end of the bar by more than the reach.
    assert!(RULER.nearest_edge(280.0, 85.0, 10.0).is_none());
    assert!(RULER.nearest_edge(245.0, 85.0, 10.0).is_some());
}

#[test]
fn a_projected_line_is_straight_on_a_turned_ruler() {
    let ruler = Guide::Ruler {
        centre: [0.0, 0.0],
        angle: 30f32.to_radians(),
        length: 300.0,
        thickness: 30.0,
    };
    let (edge, _) = ruler.nearest_edge(10.0, 20.0, 12.0).expect("near an edge");
    let a = edge.project(10.0, 20.0, 1.5);
    let b = edge.project(80.0, 90.0, 1.5);
    let c = edge.project(-40.0, 5.0, 1.5);
    let cross = (b.0 - a.0) * (c.1 - a.1) - (b.1 - a.1) * (c.0 - a.0);
    assert!(cross.abs() < 1e-2, "{cross}");
    let angle = (b.1 - a.1).atan2(b.0 - a.0).to_degrees();
    assert!((angle - 30.0).abs() < 1e-3, "{angle}");
}

#[test]
fn the_nearer_of_two_guides_wins() {
    let far = Guide::Ruler {
        centre: [100.0, 100.0],
        angle: 0.0,
        length: 300.0,
        thickness: 6.0,
    };
    let edge = nearest_edge(&[RULER, far], 100.0, 96.0, 10.0).expect("an edge");
    // The thin ruler's top edge at y = 97 is closer than y = 85.
    assert!((edge.project(100.0, 96.0, 0.0).1 - 97.0).abs() < 1e-4);
    assert!(nearest_edge(&[], 0.0, 0.0, 10.0).is_none());
}

#[test]
fn the_protractor_arc_projects_onto_a_circle_outside_it() {
    let p = protractor(0.0);
    let (edge, d) = p.nearest_edge(0.0, -96.0, 10.0).expect("near the arc");
    assert!((d - 4.0).abs() < 1e-4);
    let (x, y) = edge.project(30.0, -98.0, 2.0);
    assert!((x.hypot(y) - 102.0).abs() < 1e-3);
    // Every point of a sweep lies on the same circle.
    for deg in 0..=180 {
        let t = (deg as f32).to_radians();
        let (x, y) = edge.project(97.0 * t.cos(), -97.0 * t.sin(), 2.0);
        assert!((x.hypot(y) - 102.0).abs() < 1e-3);
    }
}

#[test]
fn the_protractor_base_is_a_line_and_the_centre_stays_free() {
    let p = protractor(0.0);
    let (edge, _) = p.nearest_edge(60.0, 4.0, 10.0).expect("near the base");
    // Ink goes below the base, away from the disc.
    assert!((edge.project(60.0, 4.0, 2.0).1 - 2.0).abs() < 1e-4);
    assert!(p.nearest_edge(3.0, 2.0, 10.0).is_none());
    assert!(p.centre_near(3.0, 2.0, 10.0));
    assert!(!p.centre_near(60.0, 0.0, 10.0));
    assert!(!RULER.centre_near(100.0, 100.0, 10.0));
    // Below the base and away from the arc is the desk.
    assert!(p.nearest_edge(60.0, 30.0, 10.0).is_none());
}

#[test]
fn the_protractor_follows_its_turn() {
    let p = protractor(90f32.to_radians());
    // Turned a quarter clockwise, the base runs down the page.
    let (edge, _) = p.nearest_edge(-3.0, 60.0, 10.0).expect("near the base");
    let (x, y) = edge.project(-3.0, 60.0, 1.0);
    assert!((x - -1.0).abs() < 1e-3, "{x}");
    assert!((y - 60.0).abs() < 1e-3);
}

#[test]
fn the_reading_is_the_angle_from_the_base() {
    let p = protractor(0.0);
    let at = |deg: f32| {
        let t = deg.to_radians();
        p.reading_degrees(50.0 * t.cos(), -50.0 * t.sin())
            .expect("off the centre")
    };
    for deg in [0.0, 37.0, 90.0, 143.0, 180.0] {
        assert!((at(deg) - deg).abs() < 1e-3, "{deg}");
    }
    assert!(p.reading_degrees(0.0, 0.0).is_none());
    assert!(RULER.reading_degrees(1.0, 1.0).is_none());
    let turned = protractor(30f32.to_radians());
    let t = 30f32.to_radians();
    let r = turned.reading_degrees(50.0 * t.cos(), 50.0 * t.sin());
    assert!(r.is_some_and(|r| r.abs() < 1e-3));
}

#[test]
fn rotation_snaps_to_whole_degrees_and_gently_to_fifteens() {
    let deg = |d: f32| snap_angle(d.to_radians()).to_degrees().round();
    assert_eq!(deg(36.6), 37.0);
    assert_eq!(deg(44.2), 45.0);
    assert_eq!(deg(46.9), 45.0);
    assert_eq!(deg(48.0), 48.0);
    assert_eq!(deg(0.9), 0.0);
    assert_eq!(deg(-29.0), -30.0);
    assert_eq!(deg(91.4), 90.0);
    assert_eq!(deg(20.0), 20.0);
    assert_eq!(deg(62.0), 60.0);
}

#[test]
fn the_ruler_label_counts_anticlockwise_from_the_horizontal() {
    assert_eq!(ruler_degrees(0.0), 0.0);
    assert_eq!(ruler_degrees((-37f32).to_radians()), 37.0);
    assert_eq!(ruler_degrees(37f32.to_radians()), 143.0);
    assert_eq!(ruler_degrees(PI), 0.0);
    assert_eq!(ruler_degrees(-PI / 2.0), 90.0);
}

#[test]
fn guides_unpack_and_skip_what_they_cannot_read() {
    let data = [
        0.0, 1.0, 2.0, 0.5, 300.0, 30.0, // a ruler
        1.0, 3.0, 4.0, 0.0, 100.0, 0.0, // a protractor
        7.0, 0.0, 0.0, 0.0, 1.0, 1.0, // unknown kind
        0.0, 0.0, 0.0, 0.0, 0.0, 30.0, // no length
        1.0, 2.0, // partial
    ];
    let guides = Guide::unpack(&data);
    assert_eq!(guides.len(), 2);
    assert!(matches!(guides[1], Guide::Protractor { radius, .. } if radius == 100.0));
}

#[test]
fn arcs_are_filled_in_between_distant_points() {
    let edge = GuideEdge::Circle {
        centre: [0.0, 0.0],
        radius: 100.0,
    };
    let a = (100.0, 0.0);
    let b = (0.0, 100.0);
    let n = edge.segments(a, b);
    assert!(n >= 60, "{n}");
    for i in 1..n {
        let (x, y) = edge.between(a, b, i as f32 / n as f32);
        assert!((x.hypot(y) - 100.0).abs() < 1e-2);
    }
    let end = edge.between(a, b, 1.0);
    assert!((end.0 - b.0).abs() < 1e-2 && (end.1 - b.1).abs() < 1e-2);
    let line = GuideEdge::Line {
        origin: [0.0, 0.0],
        dir: [1.0, 0.0],
        outward: [0.0, 1.0],
    };
    assert_eq!(line.segments(a, b), 1);
}
