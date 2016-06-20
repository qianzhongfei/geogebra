package org.geogebra.common.gui.dialog.options.model;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.App;

public class FixObjectModel extends BooleanOptionModel {

	public FixObjectModel(IBooleanOptionListener listener, App app) {
		super(listener, app);
	}

	public void apply(int index, boolean value) {
		GeoElement geo = getGeoAt(index);
		geo.setFixed(value);
		geo.updateRepaint();
	}

	@Override
	public boolean isValidAt(int index) {
		return getGeoAt(index).isFixable();
	}

	@Override
	public boolean getValueAt(int index) {
		return getGeoAt(index).isFixed();
	}
}
