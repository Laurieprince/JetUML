/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2020, 2021 by McGill University.
 *     
 * See: https://github.com/prmr/JetUML
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *******************************************************************************/
package org.jetuml.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractValidator<T> implements Validator
{
	private Optional<T> aObject;
	private Optional<List<String>> aErrors = Optional.of(new ArrayList<String>());
	
	@Override
	public boolean isValid()
	{
		return aErrors.get().isEmpty();
	}

	@Override
	public List<String> errors()
	{
		assert !isValid();
		return aErrors.get();
	}

	@Override
	public T object()
	{
		assert isValid() && aObject.isPresent();
		return aObject.get();
	}
	
	public void setObject(T pObject)
	{
		assert pObject != null;
		aObject = Optional.of(pObject);
	}
	
	public void addError(String pError)
	{
		aErrors.get().add(pError);
	}
}
