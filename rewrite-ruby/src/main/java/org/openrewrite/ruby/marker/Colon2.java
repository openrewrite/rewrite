package org.openrewrite.ruby.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * When calling a method, you may also use :: to designate the receiver,
 * but this is rarely used due to the potential for confusion with :: for namespaces.
 * For example both of these are the same:<br/>
 * {@code Nokogiri::XML(response.body)}<br/>
 * {@code Nokogiri.XML(response.body)}
 */
@Value
@With
public class Colon2 implements Marker {
    UUID id;
}
